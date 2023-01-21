import SwiftUI
import shared

struct ContentView: View {
    @State private var count: Int = 0
    
	let greet = Greeting().greet()
    let apiCaller = ApiCaller()

	var body: some View {
        VStack(spacing: 20) {
            Text(greet)
            
            HStack {
                Button(action: {
                    count -= 1
                    Task {
                        try await apiCaller.postCount(number: Int32(count))
                    }
                }) {
                    Text("-")
                }
                
                Text(String(count))
                
                Button(action: {
                    count += 1
                    Task {
                        try await apiCaller.postCount(number: Int32(count))
                    }
                }) {
                    Text("+")
                }
            }
        }
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
