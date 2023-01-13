//
//  FrameSender.swift
//  ScreenCapture
//
//  Created by afterain.cc on 2023/1/7.
//

import Foundation
import Network
import CoreMedia
import CoreImage
import UIKit

class FrameSender {
    var conn: NWConnection!
    var sending: Bool = false
    var receving: Bool = false
    var connecting: Bool = false
    var syncMode: Bool = true
    var sendReady: Bool = true
    var recvReady: Bool = true
    let q: DispatchQueue = DispatchQueue(label: "TCP Client Queue")
    
    func start(_ ip: String, syncMode: Bool = true) {
        self.syncMode = syncMode
        
        connecting = true
        let h = NWEndpoint.Host(ip)
        conn = NWConnection(to: NWEndpoint.hostPort(host: h, port: 12345), using: NWParameters.tcp)
        conn.stateUpdateHandler = { (newState) in
            switch (newState) {
            case .ready:
                self.connecting = false
                print("connect ready")
            case .failed(_):
                self.connecting = false
                print("connect failed")
            case .cancelled:
                self.connecting = false
                print("connect cancelled")
            @unknown default:
                print("connect unknown")
                self.connecting = false
            }
        }
        conn.start(queue: q)
        
        while syncMode && connecting {
            sleep(1)
        }
    }
    
    func stop() {
        conn.forceCancel()
    }
    
    func connected() -> Bool {
        conn.state == .ready
    }
    
    func sendFrame(_ sampleBuffer: CMSampleBuffer) {
        if !connected() {
            return
        }
        if !syncMode {
            if !sendReady {
                return
            }
            if !recvReady {
                return
            }
        }
        
        let imageBuffer:CVPixelBuffer = sampleBuffer.imageBuffer!
        let ciImage:CIImage = CIImage(cvPixelBuffer: imageBuffer)
        let scaleFactor = 240/ciImage.extent.width
        let cropCIImage:CIImage = ciImage.cropped(to: CGRect(x: 0, y: ciImage.extent.height-ciImage.extent.width,
                                                             width: ciImage.extent.width,
                                                             height: ciImage.extent.width))
            .transformed(by: CGAffineTransform(scaleX: scaleFactor, y: scaleFactor))
        let context:CIContext = CIContext.init(options: nil)
        let cgImage:CGImage = context.createCGImage(cropCIImage, from: cropCIImage.extent)!
        let uiImage:UIImage = UIImage.init(cgImage: cgImage)
        let data:Data = uiImage.jpegData(compressionQuality: 0.6)!

        print("sendFrame: ", data.count)
        sending = true
        sendReady = false
        recvReady = false
        conn.send(content: data, completion: .contentProcessed { (sendError) in
            if let e = sendError {
                print("sendFrame: send exception", e)
                self.sending = false
            } else {
                self.sending = false
                self.sendReady = true
            }
        })
        
        while syncMode && connected() && sending {
            sleep(1)
        }
    }
    
    func recvAck() {
        if !connected() {
            return
        }
        if !syncMode {
            if !sendReady {
                return
            }
        }

        receving = true
        conn.receive(minimumIncompleteLength: 3, maximumLength: 3) { (content, contentContext, isComplete, recvError) in
            if let e = recvError {
                print("sendFrame: recv exception", e)
                self.receving = false
            } else {
                self.receving = false
                self.recvReady = true
                let ack = String(data: content ?? "nil".data(using: .utf8)!, encoding: .utf8)
                print("receiveAck: ", ack!)
            }
        }
        
        while syncMode && connected() && receving {
            sleep(1)
        }
    }
}
