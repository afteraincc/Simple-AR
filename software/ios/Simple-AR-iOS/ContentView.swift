//
//  ContentView.swift
//  Simple-AR-iOS
//
//  Created by afterain.cc on 2023/1/1.
//

import SwiftUI
import SystemConfiguration.CaptiveNetwork
import CoreLocation
import NetworkExtension
import ReplayKit

class HotspotStatus: ObservableObject {
    @Published var hasSSIDs = false
    
    func getSSIDs() {
        NEHotspotConfigurationManager.shared.getConfiguredSSIDs { (ssidsArray) in
            print("ssidsArray.count \(ssidsArray.count)")
            for s in ssidsArray {
                print("connected ssid \(s)")
            }
            if ssidsArray.count > 0 {
                self.hasSSIDs = true
            }
        }
    }
}

struct SystemBroadcastPickerView: UIViewRepresentable {
    typealias UIViewType = RPSystemBroadcastPickerView
    
    func makeUIView(context: Context) -> UIViewType {
        let pickerView = RPSystemBroadcastPickerView(frame: CGRect(x: 0, y: 0, width: 10, height: 10))
        pickerView.preferredExtension = "cc.afterain.Simple-AR-iOS.ScreenCapture"
        pickerView.showsMicrophoneButton = false
        return pickerView
    }

    func updateUIView(_ uiView: UIViewType, context: Context) {}
}

struct ContentView: View {
    @State var ip = "172.20."
    @StateObject var hotspotStatus = HotspotStatus()

    var body: some View {
        VStack {
            Toggle("热点开启状态", isOn: $hotspotStatus.hasSSIDs)
                .disabled(false)
                .padding()
            HStack {
                Text("IP地址:")
                TextField("输入IP地址", text: $ip)
                    .onChange(of: ip, perform: { value in
                        let defaults:UserDefaults! = UserDefaults(suiteName: "group.cc.afterain.ScreenCapture")
                        if defaults != nil {
                            defaults.set(value, forKey: "ip")
                            defaults.synchronize()
                        }
                    })
            }.padding()
            SystemBroadcastPickerView()
                .frame(width: 50, height: 50)
                .background(Color.gray)
        }.onAppear() {
            hotspotStatus.getSSIDs()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
