//
//  SampleHandler.swift
//  ScreenCapture
//
//  Created by afterain.cc on 2023/1/7.
//

import ReplayKit

class SampleHandler: RPBroadcastSampleHandler {
    var ip:String = ""
    var frameSender: FrameSender = FrameSender()
    
    override func broadcastStarted(withSetupInfo setupInfo: [String : NSObject]?) {
        // User has requested to start the broadcast. Setup info from the UI extension can be supplied but optional. 
    }
    
    override func broadcastPaused() {
        // User has requested to pause the broadcast. Samples will stop being delivered.
    }
    
    override func broadcastResumed() {
        // User has requested to resume the broadcast. Samples delivery will resume.
    }
    
    override func broadcastFinished() {
        // User has requested to finish the broadcast.
    }
    
    override func processSampleBuffer(_ sampleBuffer: CMSampleBuffer, with sampleBufferType: RPSampleBufferType) {
        switch sampleBufferType {
        case RPSampleBufferType.video:
            // Handle video sample buffer
            if ip.isEmpty {
                let defaults:UserDefaults! = UserDefaults(suiteName: "group.cc.afterain.ScreenCapture")
                if defaults != nil {
                    let ipObject = defaults.object(forKey: "ip")
                    if ipObject != nil {
                         ip = ipObject as! String
                    }
                }
                if !ip.isEmpty {
                    frameSender.start(ip, syncMode: false)
                }
            } else {
                if frameSender.connected() {
                    frameSender.sendFrame(sampleBuffer)
                    frameSender.recvAck()
                }
            }
            break
        case RPSampleBufferType.audioApp:
            // Handle audio sample buffer for app audio
            break
        case RPSampleBufferType.audioMic:
            // Handle audio sample buffer for mic audio
            break
        @unknown default:
            // Handle other sample buffer types
            fatalError("Unknown type of sample buffer")
        }
    }
}
