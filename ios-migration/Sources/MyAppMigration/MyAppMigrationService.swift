import Foundation

public struct MyAppImportConflict {
    public let type: String
    public let id: String
    public let reason: String
}

public struct MyAppImportPreview {
    public let manifest: [String: Any]
    public let records: [CanonicalMigrationRecord]
    public let countsByType: [String: Int]
    public let conflicts: [MyAppImportConflict]
    public let errors: [String]
}

public struct MyAppImportSummary {
    public var inserted = 0
    public var updated = 0
    public var skipped = 0
    public var conflicts = 0
    public var errors = 0
}

public protocol CoreDataCanonicalMigrationBridge {
    func exportCanonicalRecords() throws -> [CanonicalMigrationRecord]
    func findConflicts(for records: [CanonicalMigrationRecord]) throws -> [MyAppImportConflict]
    func importCanonicalRecordsSkippingExisting(_ records: [CanonicalMigrationRecord]) throws -> MyAppImportSummary
}

public final class MyAppMigrationService {
    private let bridge: CoreDataCanonicalMigrationBridge
    private let crypto: MyAppMigrationCrypto
    private let sourceAppVersion: String

    public init(bridge: CoreDataCanonicalMigrationBridge, sourceAppVersion: String, crypto: MyAppMigrationCrypto = MyAppMigrationCrypto()) {
        self.bridge = bridge
        self.sourceAppVersion = sourceAppVersion
        self.crypto = crypto
    }

    public func exportFile(password: String) throws -> Data {
        let records = try bridge.exportCanonicalRecords()
        let counts = Dictionary(grouping: records, by: { $0.type }).mapValues(\.count)
        let parameters = try crypto.newParameters()
        let manifest = try buildManifest(counts: counts, parameters: parameters)
        let ndjson = try records.map { try $0.jsonLine() }.joined(separator: "\n") + "\n"
        let plaintext = try MyAppMigrationFormat.pack(manifest: manifest, ndjson: ndjson)
        return try crypto.encrypt(plaintext: plaintext, password: password, parameters: parameters)
    }

    public func previewImport(fileData: Data, password: String) throws -> MyAppImportPreview {
        let package = try crypto.decrypt(fileData: fileData, password: password)
        try validateManifest(package.manifest)
        var records: [CanonicalMigrationRecord] = []
        var errors: [String] = []
        package.ndjson.enumerateLines { line, _ in
            guard !line.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
            do {
                records.append(try CanonicalMigrationRecord.parse(line))
            } catch {
                errors.append(error.localizedDescription)
            }
        }
        let counts = Dictionary(grouping: records, by: { $0.type }).mapValues(\.count)
        let conflicts = try bridge.findConflicts(for: records)
        return MyAppImportPreview(manifest: package.manifest, records: records, countsByType: counts, conflicts: conflicts, errors: errors)
    }

    public func importPreviewSkippingExisting(_ preview: MyAppImportPreview) throws -> MyAppImportSummary {
        guard preview.errors.isEmpty else {
            throw MyAppMigrationError.invalidRecord("Hay errores de validacion pendientes")
        }
        return try bridge.importCanonicalRecordsSkippingExisting(preview.records)
    }

    private func buildManifest(counts: [String: Int], parameters: MyAppEncryptionParameters) throws -> [String: Any] {
        [
            "format": MyAppMigrationFormat.formatName,
            "formatVersion": MyAppMigrationFormat.formatVersion,
            "createdAt": MyAppMigrationFormat.iso8601UTC(),
            "sourcePlatform": "ios",
            "sourceAppVersion": sourceAppVersion,
            "schemaVersion": MyAppMigrationFormat.schemaVersion,
            "contentSummary": counts,
            "encryption": crypto.encryptionMetadata(parameters),
            "exportId": UUID().uuidString
        ]
    }

    private func validateManifest(_ manifest: [String: Any]) throws {
        guard manifest["format"] as? String == MyAppMigrationFormat.formatName,
              manifest["formatVersion"] as? Int == MyAppMigrationFormat.formatVersion,
              (manifest["schemaVersion"] as? Int ?? 0) <= MyAppMigrationFormat.schemaVersion,
              let sourcePlatform = manifest["sourcePlatform"] as? String,
              ["ios", "android"].contains(sourcePlatform),
              manifest["contentSummary"] is [String: Any],
              manifest["exportId"] as? String != nil else {
            throw MyAppMigrationError.unsupportedFormat("Manifest no soportado")
        }
    }
}
