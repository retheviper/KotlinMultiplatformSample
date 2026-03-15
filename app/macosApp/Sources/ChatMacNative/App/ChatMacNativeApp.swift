import AppKit
import SwiftUI

final class ChatMacNativeAppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        if let iconURL = Bundle.module.url(forResource: "AppIcon", withExtension: "png"),
           let icon = NSImage(contentsOf: iconURL) {
            NSApp.applicationIconImage = icon
        }
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
        NSApp.windows.first?.makeKeyAndOrderFront(nil)
    }
}

@main
struct ChatMacNativeApp: App {
    @NSApplicationDelegateAdaptor(ChatMacNativeAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup("Chat Desktop") {
            MacNativeRootView()
        }
        .defaultSize(width: 1440, height: 960)
        .windowResizability(.contentSize)
    }
}
