# AI Agent (Mobile App) — Setup Guide

Ye ek Android app hai jisme:
- **Home screen icon** hota hai (normal app ki tarah)
- Neeche **text box + mic button** hai — text type karein ya bol kar command dein
- Search results seedha app ke andar (WebView) dikhte hain
- Direct file links (video/pdf/image jo `.mp4`, `.pdf` waghera par khatam hon) download bhi ho sakti hain

## Kaise chalayein

1. Android Studio mein `File > Open` se `AgentApp` folder select karein
2. Gradle sync hone dein
3. Phone/emulator par Run karein (green ▶ button)

## Use kaise karein

- **Text se:** neeche box mein likhein, jaise `search OFWM field manual volume 3`, aur send (paper plane icon) dabayein
- **Voice se:** mic icon dabayein, bolen, Android khud text mein convert kar dega aur turant search chala dega
- **Download:** koi direct file link paste karein (jaise `.mp4` ya `.pdf` par khatam hone wala URL) — ye seedha phone ke Downloads folder mein save ho jayega, notification bhi aayegi

## Ek Honest Limitation

Ye mobile app **direct file links** (jo seedha .mp4/.pdf/.jpg par khatam hote hain) download kar sakti hai.

Lekin **YouTube, Instagram, TikTok jaisi pages** (jahan video HTML page ke andar chupi hoti hai, direct link nahi hota) ke liye extraction technology chahiye (`yt-dlp` jaisi library), jo Android par native tareeke se lagana bohat heavy hota hai (Python runtime bundle karna padta hai).

**Iska hal:** aapke paas jo pehle wala **ResearchAgent (PC/laptop Python tool)** hai, wo `yt-dlp` use karta hai aur YouTube/Instagram/TikTok jaisi saari sites handle kar leta hai. Mobile app se aap search kar sakte hain aur direct links download kar sakte hain; social-media-style pages ke liye wahi PC tool use karein — ya bata dein to is mobile app mein ek "Share to PC agent" jodne ka tareeka bhi bana dun.

## Permissions

- **Internet** — search aur download ke liye
- **Microphone** — sirf voice command ke waqt, Android ka built-in speech recognizer use hota hai (koi audio kahin store nahi hoti)
