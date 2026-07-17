import CommonCrypto
import CryptoKit
import Foundation
import Security

public struct MyAppEncryptionParameters {
    public let salt: Data
    public let nonce: Data
}

public final class MyAppMigrationCrypto {
    public init() {}

    public func newParameters() throws -> MyAppEncryptionParameters {
        MyAppEncryptionParameters(
            salt: try randomData(count: MyAppMigrationFormat.saltBytes),
            nonce: try randomData(count: MyAppMigrationFormat.nonceBytes)
        )
    }

    public func encryptionMetadata(_ parameters: MyAppEncryptionParameters) -> [String: Any] {
        [
            "cipher": MyAppMigrationFormat.cipher,
            "kdf": MyAppMigrationFormat.kdf,
            "kdfIterations": MyAppMigrationFormat.kdfIterations,
            "salt": parameters.salt.base64EncodedString(),
            "nonce": parameters.nonce.base64EncodedString(),
            "keyLengthBits": MyAppMigrationFormat.keyBits,
            "tagLengthBits": MyAppMigrationFormat.tagBits,
            "argon2idAvailable": false
        ]
    }

    public func encrypt(plaintext: Data, password: String, parameters: MyAppEncryptionParameters) throws -> Data {
        let headerData = try header(parameters)
        let key = try deriveKey(password: password, salt: parameters.salt)
        let sealed = try AES.GCM.seal(
            plaintext,
            using: key,
            nonce: AES.GCM.Nonce(data: parameters.nonce),
            authenticating: headerData
        )
        var encryptedPayload = Data()
        encryptedPayload.append(sealed.ciphertext)
        encryptedPayload.append(sealed.tag)
        var file = Data()
        file.append(MyAppMigrationFormat.magic.data(using: .utf8)!)
        file.append(0x0A)
        file.append(headerData)
        file.append(0x0A)
        file.append(encryptedPayload)
        return file
    }

    public func decrypt(fileData: Data, password: String) throws -> (manifest: [String: Any], ndjson: String) {
        let parts = fileData.split(separator: 0x0A, maxSplits: 2, omittingEmptySubsequences: false)
        guard parts.count == 3, String(data: Data(parts[0]), encoding: .utf8) == MyAppMigrationFormat.magic else {
            throw MyAppMigrationError.invalidFile("Formato de archivo no reconocido")
        }
        let headerData = Data(parts[1])
        guard let header = try JSONSerialization.jsonObject(with: headerData) as? [String: Any] else {
            throw MyAppMigrationError.invalidFile("Cabecera de cifrado invalida")
        }
        try validateHeader(header)
        guard let saltText = header["salt"] as? String,
              let nonceText = header["nonce"] as? String,
              let salt = Data(base64Encoded: saltText),
              let nonce = Data(base64Encoded: nonceText) else {
            throw MyAppMigrationError.invalidFile("Salt o nonce invalidos")
        }
        let key = try deriveKey(password: password, salt: salt)
        do {
            let encryptedPayload = Data(parts[2])
            guard encryptedPayload.count > MyAppMigrationFormat.tagBits / 8 else {
                throw MyAppMigrationError.invalidFile("Ciphertext AES-GCM incompleto")
            }
            let tagStart = encryptedPayload.count - (MyAppMigrationFormat.tagBits / 8)
            let ciphertext = encryptedPayload.prefix(tagStart)
            let tag = encryptedPayload.suffix(MyAppMigrationFormat.tagBits / 8)
            let box = try AES.GCM.SealedBox(
                nonce: AES.GCM.Nonce(data: nonce),
                ciphertext: ciphertext,
                tag: tag
            )
            let plaintext = try AES.GCM.open(
                box,
                using: key,
                authenticating: headerData
            )
            return try MyAppMigrationFormat.unpack(plaintext)
        } catch {
            throw MyAppMigrationError.wrongPasswordOrCorruptFile
        }
    }

    private func header(_ parameters: MyAppEncryptionParameters) throws -> Data {
        let object: [String: Any] = [
            "format": MyAppMigrationFormat.formatName,
            "formatVersion": MyAppMigrationFormat.formatVersion,
            "cipher": MyAppMigrationFormat.cipher,
            "kdf": MyAppMigrationFormat.kdf,
            "kdfIterations": MyAppMigrationFormat.kdfIterations,
            "salt": parameters.salt.base64EncodedString(),
            "nonce": parameters.nonce.base64EncodedString(),
            "keyLengthBits": MyAppMigrationFormat.keyBits,
            "tagLengthBits": MyAppMigrationFormat.tagBits
        ]
        return try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
    }

    private func validateHeader(_ header: [String: Any]) throws {
        guard header["format"] as? String == MyAppMigrationFormat.formatName,
              header["formatVersion"] as? Int == MyAppMigrationFormat.formatVersion,
              header["cipher"] as? String == MyAppMigrationFormat.cipher,
              header["kdf"] as? String == MyAppMigrationFormat.kdf,
              header["kdfIterations"] as? Int == MyAppMigrationFormat.kdfIterations,
              header["keyLengthBits"] as? Int == MyAppMigrationFormat.keyBits,
              header["tagLengthBits"] as? Int == MyAppMigrationFormat.tagBits else {
            throw MyAppMigrationError.unsupportedFormat("Cabecera no soportada")
        }
    }

    private func deriveKey(password: String, salt: Data) throws -> SymmetricKey {
        guard let passwordData = password.data(using: .utf8), !passwordData.isEmpty else {
            throw MyAppMigrationError.invalidFile("La contraseña no puede estar vacia")
        }
        var key = Data(count: MyAppMigrationFormat.keyBits / 8)
        let result = key.withUnsafeMutableBytes { keyBytes in
            salt.withUnsafeBytes { saltBytes in
                passwordData.withUnsafeBytes { passwordBytes in
                    CCKeyDerivationPBKDF(
                        CCPBKDFAlgorithm(kCCPBKDF2),
                        passwordBytes.bindMemory(to: Int8.self).baseAddress,
                        passwordData.count,
                        saltBytes.bindMemory(to: UInt8.self).baseAddress,
                        salt.count,
                        CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                        UInt32(MyAppMigrationFormat.kdfIterations),
                        keyBytes.bindMemory(to: UInt8.self).baseAddress,
                        key.count
                    )
                }
            }
        }
        guard result == kCCSuccess else {
            throw MyAppMigrationError.invalidFile("No se pudo derivar la clave")
        }
        return SymmetricKey(data: key)
    }

    private func randomData(count: Int) throws -> Data {
        var data = Data(count: count)
        let status = data.withUnsafeMutableBytes {
            SecRandomCopyBytes(kSecRandomDefault, count, $0.bindMemory(to: UInt8.self).baseAddress!)
        }
        guard status == errSecSuccess else {
            throw MyAppMigrationError.invalidFile("No se pudo generar aleatoriedad segura")
        }
        return data
    }
}
