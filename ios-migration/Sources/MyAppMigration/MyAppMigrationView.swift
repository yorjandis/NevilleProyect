import SwiftUI
import UniformTypeIdentifiers

public struct MyAppMigrationView: View {
    @State private var password = ""
    @State private var status = ""
    @State private var preview: MyAppImportPreview?
    private let service: MyAppMigrationService

    public init(service: MyAppMigrationService) {
        self.service = service
    }

    public var body: some View {
        Form {
            Section("Migración Android / iOS") {
                SecureField("Contraseña", text: $password)
                ShareLink(
                    "Exportar a Android",
                    item: ExportPayload(service: service, password: password),
                    preview: SharePreview("Neville\(MyAppMigrationFormat.fileExtension)")
                )
                Text("Para importar, selecciona el archivo .ypgexp desde el document picker de tu app y llama a previewImport(fileData:password:).")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                if !status.isEmpty {
                    Text(status).font(.footnote)
                }
            }
        }
        .navigationTitle("Migración")
    }
}

private struct ExportPayload: Transferable {
    let service: MyAppMigrationService
    let password: String

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .data) { payload in
            try payload.service.exportFile(password: payload.password)
        }
        .suggestedFileName("neville-\(Int(Date().timeIntervalSince1970))\(MyAppMigrationFormat.fileExtension)")
    }
}
