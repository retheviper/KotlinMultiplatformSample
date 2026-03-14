import SwiftUI

struct MacNativeRootView: View {
    @StateObject private var store = ChatStore(
        baseURLString: ProcessInfo.processInfo.environment["MESSAGING_BASE_URL"] ?? "http://localhost:8080"
    )

    var body: some View {
        Group {
            switch store.screen {
            case .landing:
                LandingView(store: store)
            case .memberSetup:
                MemberSetupView(store: store)
            case .workspace:
                WorkspaceShellView(store: store)
            }
        }
        .frame(minWidth: 1180, minHeight: 760)
        .background(Color(nsColor: .windowBackgroundColor))
        .task {
            NotificationManager.shared.requestAuthorizationIfNeeded()
            await store.connectServer()
        }
        .overlay(alignment: .topTrailing) {
            ToastStackView(store: store)
                .padding(20)
        }
        .alert("Error", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {
                store.errorMessage = nil
            }
        } message: {
            Text(store.errorMessage ?? "")
        }
    }
}
