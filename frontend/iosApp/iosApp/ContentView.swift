import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // NOTE: do NOT add .ignoresSafeArea(.keyboard) here. Ignoring the keyboard safe area
        // makes the Compose host view extend underneath the on-screen keyboard, so the insets
        // that Compose's imePadding() relies on are never delivered — the keyboard then overlaps
        // text fields and CTAs on every screen (see ConfigureCategoriesScreen). Without the
        // modifier, SwiftUI resizes the host view to sit above the keyboard and imePadding()
        // shrinks scrollable content as intended.
        ComposeView()
    }
}
