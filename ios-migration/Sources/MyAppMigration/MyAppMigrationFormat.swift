import Foundation

public enum MyAppMigrationFormat {
    public static let fileExtension = ".ypgexp"
    public static let formatName = "com.ypg.neville.ndjson.export"
    public static let formatVersion = 1
    public static let schemaVersion = 1
    public static let magic = "MYAPPEXPORT-1"
    public static let cipher = "AES-256-GCM"
    public static let kdf = "PBKDF2-HMAC-SHA256"
    public static let kdfIterations = 310_000
    public static let saltBytes = 16
    public static let nonceBytes = 12
    public static let keyBits = 256
    public static let tagBits = 128

    public static func iso8601UTC(_ date: Date = Date()) -> String {
        ISO8601DateFormatter().string(from: date)
    }

    public static func date(from value: String) throws -> Date {
        guard let date = ISO8601DateFormatter().date(from: value) else {
            throw MyAppMigrationError.invalidRecord("Fecha ISO-8601 invalida: \(value)")
        }
        return date
    }

    public static func pack(manifest: [String: Any], ndjson: String) throws -> Data {
        let manifestData = try JSONSerialization.data(withJSONObject: manifest, options: [.sortedKeys])
        var result = Data()
        var length = UInt32(manifestData.count).bigEndian
        result.append(Data(bytes: &length, count: 4))
        result.append(manifestData)
        result.append(ndjson.data(using: .utf8) ?? Data())
        return result
    }

    public static func unpack(_ data: Data) throws -> (manifest: [String: Any], ndjson: String) {
        guard data.count >= 4 else { throw MyAppMigrationError.invalidFile("Paquete descifrado incompleto") }
        let length = data.prefix(4).reduce(UInt32(0)) { ($0 << 8) | UInt32($1) }
        let manifestLength = Int(length)
        guard manifestLength > 0, data.count >= 4 + manifestLength else {
            throw MyAppMigrationError.invalidFile("Tamanho de manifiesto invalido")
        }
        let manifestData = data.subdata(in: 4..<(4 + manifestLength))
        let payloadData = data.subdata(in: (4 + manifestLength)..<data.count)
        guard let manifest = try JSONSerialization.jsonObject(with: manifestData) as? [String: Any] else {
            throw MyAppMigrationError.invalidFile("manifest.json invalido")
        }
        let ndjson = String(data: payloadData, encoding: .utf8) ?? ""
        return (manifest, ndjson)
    }
}

public enum MyAppMigrationError: Error, LocalizedError {
    case invalidFile(String)
    case invalidRecord(String)
    case unsupportedFormat(String)
    case wrongPasswordOrCorruptFile

    public var errorDescription: String? {
        switch self {
        case .invalidFile(let message), .invalidRecord(let message), .unsupportedFormat(let message):
            return message
        case .wrongPasswordOrCorruptFile:
            return "Contraseña incorrecta o archivo corrupto"
        }
    }
}

public struct CanonicalMigrationRecord {
    public let type: String
    public let id: String
    public let createdAt: String
    public let updatedAt: String
    public let schemaVersion: Int
    public let payload: [String: Any]

    public init(type: String, id: String, createdAt: String, updatedAt: String, schemaVersion: Int = MyAppMigrationFormat.schemaVersion, payload: [String: Any]) {
        self.type = type
        self.id = id
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.schemaVersion = schemaVersion
        self.payload = payload
    }

    public func jsonLine() throws -> String {
        let object: [String: Any] = [
            "type": type,
            "id": id,
            "createdAt": createdAt,
            "updatedAt": updatedAt,
            "schemaVersion": schemaVersion,
            "payload": payload
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
        return String(data: data, encoding: .utf8) ?? "{}"
    }

    public static func parse(_ line: String) throws -> CanonicalMigrationRecord {
        guard let data = line.data(using: .utf8),
              let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = object["type"] as? String,
              let id = object["id"] as? String,
              let createdAt = object["createdAt"] as? String,
              let updatedAt = object["updatedAt"] as? String,
              let payload = object["payload"] as? [String: Any] else {
            throw MyAppMigrationError.invalidRecord("Linea NDJSON invalida")
        }
        _ = try MyAppMigrationFormat.date(from: createdAt)
        _ = try MyAppMigrationFormat.date(from: updatedAt)
        let schemaVersion = object["schemaVersion"] as? Int ?? MyAppMigrationFormat.schemaVersion
        guard schemaVersion <= MyAppMigrationFormat.schemaVersion else {
            throw MyAppMigrationError.unsupportedFormat("schemaVersion no soportado: \(schemaVersion)")
        }
        return CanonicalMigrationRecord(type: type, id: id, createdAt: createdAt, updatedAt: updatedAt, schemaVersion: schemaVersion, payload: payload)
    }
}
