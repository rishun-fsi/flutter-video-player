//
//  NativeView.swift
//  Runner
//
//  Created by lisyunn on 2021/08/11.
//

import Foundation
import Flutter
import AVKit


class NativeView: UIView, FlutterPlatformView {
    
    private let methodChannelName = "package.name/video_player"
    private let methodMsg = "controlPlayer"
    
    // MethodChnnelの結果通知に使います
    private var result: FlutterResult?
    
    private var messenger: FlutterBinaryMessenger
    private var url: String?
    let player = AVPlayer()
    let playerLayer = AVPlayerLayer()
    var playerItem: AVPlayerItem?
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("not implemented")
    }
    
    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?,
        binaryMessenger messenger: FlutterBinaryMessenger?
    ) {
        self.messenger = messenger!
        if let arguments = args as? [String:Any?] {
            self.url = arguments["url"] as? String
        }
        super.init(frame: frame)
        
    }
    
    deinit {
        playerLayer.removeObserver(self, forKeyPath: #keyPath(AVPlayerLayer.isReadyForDisplay))
        playerItem?.removeObserver(self, forKeyPath: #keyPath(AVPlayerItem.status))
    }
    
    func view() -> UIView {
        return self
    }
    
    override func layoutSubviews() {
        // iOS views can be created here
        createNativeView(view: self)
        super.layoutSubviews()
    }
    
    func createNativeView(view _view: UIView){
        
        let methodChannel = FlutterMethodChannel(name: methodChannelName, binaryMessenger: self.messenger)
        methodChannel.setMethodCallHandler { [weak self] methodCall, result in
            self?.result = result
            if methodCall.method == self?.methodMsg {
                let msg = methodCall.arguments as? String
                print(" 111111 " + msg!)
                if (msg == "pause") {
                    self?.player.pause()
                    result("play")
                } else if (msg == "play") {
                    self?.player.play()
                    result("pause")
                } else if (msg == "position") {
                    let position = (Int) (CMTimeGetSeconds((self?.player.currentTime())!))
                    result(position)
                } else if (msg == "duration") {
                    let duration = (Int) (CMTimeGetSeconds((self?.player.currentItem!.duration)!))
                    result(duration)
                    
                } else if (msg == "fast_rewind") {
                    let position = CMTimeGetSeconds((self?.player.currentTime())!)
                    let seekPos = position - 10 < 0 ? 0 : position - 10
                    self?.player.seek(to: CMTimeMakeWithSeconds(seekPos, preferredTimescale: Int32(NSEC_PER_SEC)))
                    result(seekPos)
                    
                } else if (msg == "fast_forward") {
                    let position = CMTimeGetSeconds((self?.player.currentTime())!)
                    let duration = CMTimeGetSeconds((self?.player.currentItem!.duration)!)
                    let seekPos = position + 10 > duration ? duration : position + 10
                    self?.player.seek(to: CMTimeMakeWithSeconds(seekPos, preferredTimescale: Int32(NSEC_PER_SEC)))
                    result(seekPos)
                }
            }
        }
        
        // 動画ファイルのURLを取得
        guard let url = URL(string: self.url!) else {
            return
        }
        
        
        let asset = AVAsset(url: url)
        // AVAssetをもとに、AVPlayerItemを作成する
        playerItem = AVPlayerItem(asset: asset)
        
        player.replaceCurrentItem(with: playerItem)
        
        playerLayer.player = player
        
        //        playerLayer.frame = CGRect(x: 0, y: 0, width: self.frame.size.width, height: self.frame.size.height)
        
        playerLayer.videoGravity = AVLayerVideoGravity.resizeAspect
        playerLayer.frame = self.frame
        layer.addSublayer(playerLayer)
        
        player.play()
        
        
        // register for KVO change of the ready to play property
        playerLayer.addObserver(self, forKeyPath: #keyPath(AVPlayerLayer.isReadyForDisplay), options:[.old, .new], context: nil)
        

        playerItem?.addObserver(self, forKeyPath: #keyPath(AVPlayerItem.status), options: [.old, .new], context: nil)
        
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == #keyPath(AVPlayerLayer.isReadyForDisplay) {
            FlutterBasicMessageChannel(name: methodChannelName, binaryMessenger: self.messenger).sendMessage("prepared")
        }
        
        if keyPath == #keyPath(AVPlayerItem.status) {
            let status: AVPlayerItem.Status
            if let statusNumber = change?[.newKey] as? NSNumber {
                status = AVPlayerItem.Status(rawValue: statusNumber.intValue)!
            } else {
                status = .unknown
            }
            // Switch over status value
            switch status {
            case .readyToPlay:
                // Player item is ready to play.
                break
            case .failed:
                // Player item failed. See error.
                FlutterBasicMessageChannel(name: methodChannelName, binaryMessenger: self.messenger).sendMessage("Player item failed.")
                break
            case .unknown:
                // Player item is not yet ready.
                break
            @unknown default: fatalError()
            }
        }
        
    }
    
    
}




