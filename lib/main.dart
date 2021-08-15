import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'dart:convert';

import 'package:flutter_video_player/player_page.dart';


void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp])
      .then((_) {
    runApp(MaterialApp(
        home: HomeApp()
    ));
  });
}

class HomeApp extends StatefulWidget {
  @override
  _HomeAppState createState() => _HomeAppState();
}

class _HomeAppState extends State<HomeApp> {

  @override
  void initState() {
    super.initState();
    // ローカルJSONをロード
    loadLocalJson();
  }

  List? _jsonData ; //データ

  // ローカルJSONをロード
  void loadLocalJson() async {
    try {
      String jsonString = await rootBundle.loadString('assets/json/data.json');
      setState(() {
        _jsonData = json.decode(jsonString);
      });
    }catch (e) {
      print("something went wrong" + e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(title: Text("Contents List")),
      body: ListView.builder(
        itemCount: _jsonData == null ? 0 : _jsonData!.length,
        itemBuilder: (BuildContext context, int index){
          String url = _jsonData![index]["url"];
          return InkWell(
            child: Card(
              child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: Text(
                    url,
                    style: TextStyle(
                        fontSize: 20.0
                    ),
                  )
              ),
            ),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(
                  builder: (context) => PlayerPage(url)
              ));
            }, // Handle your onTap here.
          );
        },
      ),
    );
  }
}

