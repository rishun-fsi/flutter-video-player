import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

class PlayerPage extends StatefulWidget {

  String? url;

  PlayerPage(String url) {
    this.url = url;
  }

  @override
  _PlayerPageState createState() => _PlayerPageState();
}

class _PlayerPageState extends State<PlayerPage> {

  MethodChannel _methodChannel = MethodChannel('package.name/video_player');
  BasicMessageChannel _basicMessageChannel = BasicMessageChannel('package.name/video_player', StandardMessageCodec());
  bool visibilityProgress = true;
  bool visibilityController = false;
  String playbackText = "pause";
  int position = 0;
  int duration = 0;
  String positionString = '0:00:00';
  String durationString = '';
  late BuildContext _context;
  late Timer _timer;
  int timeCount = 0;

  @override
  void initState() {
    super.initState();
    _basicMessageChannel.setMessageHandler((dynamic value) async {
      String msg = await value as String;
      if (msg == "prepared") {
        setState(() {
          visibilityProgress = false;
          visibilityController = true;
          _controlPlayer("duration");
        });
      } else {
        _showAlertDialog(_context, "ERROR", msg);
      }
      return value;
    });

    _timer = Timer.periodic(
      Duration(seconds: 1),
      _onTimer,
    );

  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  void _onTimer(Timer timer) {
    if (!visibilityProgress) {
      _controlPlayer("position");
    }

    if (timeCount > 4) {
      visibilityController = false;
      timeCount = 0;
    }
    timeCount ++;
  }

  _confirmDialog(BuildContext context, bool b) {
    setState(() {
      //
    });
    Navigator.pop(context);
  }

  Future _showAlertDialog(BuildContext context, String title, String description) async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(description),
          actions: <Widget>[
            ElevatedButton(
              child: Text('CLOSE'),
              onPressed: () => _confirmDialog(context, false),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    double width = MediaQuery.of(context).size.width;
    double height = width * 9 / 16;
    _context = context;
    return Scaffold(
      appBar: AppBar(
          title : Text("Player Page")
      ),
      body: Stack(
        // child: Column(
          children: <Widget>[
            Column(
                children: <Widget>[
                  Container(
                    color: Colors.black,
                    width: width,
                    height: height,
                    child: NativeView(url: widget.url),
                  ),
                ]
            ),
            Visibility(
                visible:visibilityProgress,
                child: Container(
                  width: width,
                  height: height,
                  alignment: Alignment.center,
                  color: Colors.black,
                  child:  CircularProgressIndicator(),
                )
            ),
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {
                if (visibilityProgress) {
                  return;
                }
                setState(() {
                  visibilityController = true;
                  timeCount = 0;
                });},
              child: Container(
                width: width,
                height: height,
                margin: const EdgeInsets.only(left: 5.0, right: 5.0),

                child: Visibility(
                  visible: visibilityController,
                  child: Material(
                    color: Colors.transparent,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: <Widget>[
                        Container(
                          margin: EdgeInsets.only(bottom: height/4),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: <Widget>[
                              IconButton(
                                onPressed: () {
                                  _controlPlayer("fast_rewind");
                                  timeCount = 0;
                                },
                                icon: Icon(Icons.fast_rewind),
                                color: Colors.red,
                                iconSize: 48,
                              ),
                              IconButton(
                                onPressed: () {
                                  _controlPlayer(playbackText);
                                  timeCount = 0;
                                },
                                icon: playbackText == "pause" ? Icon(Icons.pause_circle) : Icon(Icons.play_circle),
                                color: Colors.red,
                                iconSize: 48,
                              ),
                              IconButton(
                                onPressed: () {
                                  _controlPlayer("fast_forward");
                                  timeCount = 0;
                                },
                                icon: Icon(Icons.fast_forward ),
                                color: Colors.red,
                                iconSize: 48,
                              ),
                            ],
                          ),
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: <Widget>[
                            Text(
                              positionString,
                              style: TextStyle(
                                  color: Colors.white
                              ),
                            ),
                            Container(
                              width: width - 130,
                              height: 8,
                              child: ClipRRect(
                                borderRadius: BorderRadius.all(Radius.circular(10)),
                                child: LinearProgressIndicator(
                                    value: position == 0 ? 0.0 : position / duration,
                                    backgroundColor: Colors.grey,
                                    valueColor: AlwaysStoppedAnimation(Colors.red)),
                              ),
                            ),
                            Text(
                              durationString,
                              style: TextStyle(
                                  color: Colors.white
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ]
      ),
    );
  }


  Future<dynamic> _controlPlayer(String sendMessage) async {
    dynamic result;
    try {
      result = await _methodChannel.invokeMethod('controlPlayer', sendMessage);
    } catch (e) {
      print(e);
    }
    switch (sendMessage) {
      case "play":
      case "pause":
        setState(() {
          playbackText = result as String;
        });
        return result as String;
      case "fast_rewind":
      case "fast_forward":
        return result;
      case "position":
        position = result as int;
        int hour = position ~/ 3600;
        int minute = (position % 3600) ~/ 60;
        int second = position % 60;
        setState(() {
          positionString = "${hour.toString()}:${minute.toString().padLeft(2,'0')}:${second.toString().padLeft(2,'0')}";
        });
        return result;
      case "duration":
        duration = result as int;
        int hour = duration ~/ 3600;
        int minute = (duration % 3600) ~/ 60;
        int second = duration % 60;
        setState(() {
          durationString = "${hour.toString()}:${minute.toString().padLeft(2,'0')}:${second.toString().padLeft(2,'0')}";
        });
        return result;
      default:
        throw MissingPluginException('notImplemented');
    }

  }

}

class NativeView extends StatelessWidget {

  final String? url;

  NativeView({
    @required this.url,
  });

  @override
  Widget build(BuildContext context) {
    // This is used in the platform side to register the view.
    final String viewType = 'video_player';
    // Pass parameters to the platform side.
    final Map<String, dynamic> creationParams = <String, dynamic>{
      "url": url,
    };

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:

      // return AndroidView(
      //   viewType: viewType,
      //   layoutDirection: TextDirection.ltr,
      //   creationParams: creationParams,
      //   creationParamsCodec: const StandardMessageCodec(),
      // );

        return PlatformViewLink(
          viewType: viewType,
          surfaceFactory:
              (BuildContext context, PlatformViewController controller) {
            return AndroidViewSurface(
              controller: controller as AndroidViewController,
              gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
              hitTestBehavior: PlatformViewHitTestBehavior.opaque,
            );
          },
          onCreatePlatformView: (PlatformViewCreationParams params) {
            return PlatformViewsService.initSurfaceAndroidView(
              id: params.id,
              viewType: viewType,
              layoutDirection: TextDirection.ltr,
              creationParams: creationParams,
              creationParamsCodec: StandardMessageCodec(),
            )
              ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
              ..create();
          },
        );
      case TargetPlatform.iOS:
        return UiKitView(
          viewType: viewType,
          layoutDirection: TextDirection.ltr,
          creationParams: creationParams,
          creationParamsCodec: const StandardMessageCodec(),
        );
      default:
        throw UnsupportedError("Unsupported platform view");
    }

  }

}