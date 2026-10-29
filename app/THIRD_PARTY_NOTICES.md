# Third-party notices

## liquid glass (shuding/liquid-glass)

The glass effect in this project is a technical port of the SVG displacement-map technique from
https://github.com/shuding/liquid-glass by Shu Ding, used under the MIT licence.

Upstream files copied for reference live in `ref/upstream/`:

- `liquid-glass.js` (the flat lens variant ported to AGSL / Kotlin)
- `liquid-diamond.js` (the prism variant, ported as an analytic approximation)

```
MIT License

Copyright (c) 2025 Shu Ding

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Fonts

No font binary is bundled. The reading column asks for the generic `serif` family, which Android
resolves to NotoSerif for Latin and NotoSerifCJK (Source Han Serif, SIL Open Font License) for
Chinese, subject to the device vendor's font configuration.

## Artwork

`ref/壁纸.jpg` is the reference wallpaper supplied with this task; it ships in
`app/app/src/main/res/drawable-nodpi/wallpaper_reference.jpg`.
