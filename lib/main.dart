import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const TeluguReaderApp());

const _channel = MethodChannel('telugu_reader/settings');

class TeluguReaderApp extends StatelessWidget {
  const TeluguReaderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Telugu Reader',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.deepOrange),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool overlayGranted = false;

  Future<void> _openAccessibilitySettings() async {
    await _channel.invokeMethod('openAccessibilitySettings');
  }

  Future<void> _openOverlaySettings() async {
    await _channel.invokeMethod('openOverlaySettings');
  }

  Future<void> _checkOverlay() async {
    final granted = await _channel.invokeMethod<bool>('canDrawOverlays') ?? false;
    setState(() => overlayGranted = granted);
  }

  @override
  void initState() {
    super.initState();
    _checkOverlay();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Telugu Reader')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'ఏ app మీదైనా స్క్రీన్ మీద కనిపించే తెలుగు text ని చదివి వినిపిస్తుంది, '
              'ఆటోమేటిక్‌గా scroll కూడా చేస్తుంది.',
              style: TextStyle(fontSize: 14, color: Colors.black54),
            ),
            const SizedBox(height: 24),
            _StepCard(
              title: '1. Overlay permission',
              subtitle: overlayGranted ? 'ఇవ్వబడింది' : 'ఇవ్వలేదు',
              done: overlayGranted,
              onTap: _openOverlaySettings,
            ),
            const SizedBox(height: 12),
            _StepCard(
              title: '2. Accessibility permission',
              subtitle: 'Settings > Accessibility > Telugu Reader ఆన్ చేయండి',
              done: false,
              onTap: _openAccessibilitySettings,
            ),
            const SizedBox(height: 24),
            const Text(
              'రెండూ ఆన్ చేశాక, ఏ app తెరిచినా కుడివైపు floating bubble కనిపిస్తుంది. '
              'దాన్ని tap చేస్తే చదవడం మొదలవుతుంది, మళ్ళీ tap చేస్తే ఆగుతుంది.',
              style: TextStyle(fontSize: 13, color: Colors.black45),
            ),
          ],
        ),
      ),
    );
  }
}

class _StepCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool done;
  final VoidCallback onTap;

  const _StepCard({
    required this.title,
    required this.subtitle,
    required this.done,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text(title),
        subtitle: Text(subtitle),
        trailing: Icon(done ? Icons.check_circle : Icons.chevron_right,
            color: done ? Colors.green : Colors.black38),
        onTap: onTap,
      ),
    );
  }
}
