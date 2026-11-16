import UIKit
import SwiftUI
import ComposeApp

/// Obal Compose UI (Kotlin framework `ComposeApp`) pro SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // klávesnici řeší Compose sám
            .preferredColorScheme(.light) // světlý „papír“ → tmavý stavový řádek
    }
}
