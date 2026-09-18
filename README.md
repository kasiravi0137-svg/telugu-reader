# Telugu Reader

ఏ app మీదైనా స్క్రీన్ మీద కనిపించే తెలుగు text ని చదివి, auto-scroll చేసుకుంటూ ముందుకు సాగే accessibility app.

## GitHub నుండి APK ఎలా తీసుకోవాలి (Android Studio అవసరం లేదు)

1. **GitHub లో కొత్త repository create చేయండి** (private పెట్టుకోవచ్చు) — పేరు ఏదైనా పెట్టుకోండి, ఉదా. `telugu-reader`.

2. ఈ ఫోల్డర్ మొత్తాన్ని (`telugu_reader/` లోపల ఉన్న అన్ని files/folders) ఆ repository లోకి అప్‌లోడ్ చేయండి.
   - సులువైన మార్గం: GitHub repo page లో "Add file" → "Upload files" → ఈ ఫోల్డర్ లోని files అన్నీ డ్రాగ్ చేసి డ్రాప్ చేయండి (hidden `.github` folder కూడా వెళ్లేలా చూసుకోండి — browser upload కొన్నిసార్లు `.github` ని skip చేస్తుంది, అలాంటప్పుడు git command line వాడండి, కింద ఇచ్చాను).
   - లేదా command line తెలిస్తే:
     ```
     cd telugu_reader
     git init
     git add .
     git commit -m "initial commit"
     git branch -M main
     git remote add origin <మీ-repo-URL>
     git push -u origin main
     ```

3. అప్‌లోడ్ అయ్యాక, repo లో పైన ఉన్న **"Actions"** tab కి వెళ్ళండి. "Build APK" అనే workflow run అవ్వడం కనిపిస్తుంది (push చేసిన వెంటనే ఆటోమేటిక్‌గా మొదలవుతుంది). 3-5 నిమిషాలు పడుతుంది.

4. Run పూర్తయ్యాక (green checkmark), ఆ run ని open చేసి కిందకి scroll చేయండి — **"Artifacts"** సెక్షన్ లో `telugu-reader-apk` అని ఉంటుంది. దాన్ని tap చేస్తే ఒక `.zip` file download అవుతుంది, అందులో `app-release.apk` ఉంటుంది.

5. ఆ APK ని ఫోన్ కి పంపి (email/Drive/WhatsApp to yourself), install చేయండి. "Unknown apps" install permission అడిగితే allow చేయండి.

## App వాడే విధానం

1. App తెరిచి **Overlay permission** మరియు **Accessibility permission** రెండూ ఆన్ చేయండి (home screen లో ఉన్న రెండు cards tap చేస్తే direct గా Settings screens తెరుచుకుంటాయి).
2. ఏ app అయినా తెరవండి (Pratilipi, Chrome, ఏదైనా).
3. కుడివైపు floating bubble కనిపిస్తుంది — దాన్ని tap చేస్తే స్క్రీన్ మీద ఉన్న text చదవడం మొదలవుతుంది.
4. చదవడం పూర్తయ్యాక ఆటోమేటిక్‌గా scroll చేసి తదుపరి భాగం చదువుతుంది. Scroll ఇక సాధ్యం కాకపోతే (చివరి పేజీ వచ్చినప్పుడు) ఆగిపోతుంది.
5. బబుల్ ని మళ్ళీ tap చేస్తే ఎప్పుడైనా ఆపొచ్చు. బబుల్ ని డ్రాగ్ చేసి స్క్రీన్ మీద ఎక్కడికైనా జరపొచ్చు.

## తెలుసుకోవాల్సినవి

- **మొదటిసారి TTS వాడేటప్పుడు** Android కొన్నిసార్లు "Telugu text-to-speech data install చేయాలా" అని అడగొచ్చు — Google TTS engine లో Telugu voice pack ఇన్‌స్టాల్ చేసుకోవాలి (Settings > System > Languages > Text-to-speech లో కూడా చేయవచ్చు).
- ఏదైనా app స్క్రీన్ ని `FLAG_SECURE` తో block చేస్తే (banking apps లో సాధారణం), ఆ స్క్రీన్ మీద text చదవడం పనిచేయకపోవచ్చు.
- ఇదంతా మీ ఫోన్ లోనే జరుగుతుంది, ఎక్కడికీ డేటా పంపదు.
