"""Export the approved Android launcher artwork. Offline design tool: Pillow + PyMuPDF."""
from copy import deepcopy
from io import BytesIO
from pathlib import Path
from xml.etree import ElementTree as ET

import pymupdf
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'androidApp/src/main/res'
SOURCE = ROOT / 'docs/design/rtbus-launcher.svg'
SVG = '{http://www.w3.org/2000/svg}'
ET.register_namespace('', SVG[1:-1])
ANDROID = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', ANDROID[1:-1])


def rect_path(rect):
    x, y, w, h = (float(rect.attrib[k]) for k in ('x', 'y', 'width', 'height'))
    r = float(rect.get('rx', '0'))
    if not r:
        return f'M{x:g},{y:g}h{w:g}v{h:g}h{-w:g}Z'
    return (
        f'M{x+r:g},{y:g}H{x+w-r:g}a{r:g},{r:g} 0 0 1 {r:g},{r:g}V{y+h-r:g}'
        f'a{r:g},{r:g} 0 0 1 {-r:g},{r:g}H{x+r:g}a{r:g},{r:g} 0 0 1 {-r:g},{-r:g}'
        f'V{y+r:g}a{r:g},{r:g} 0 0 1 {r:g},{-r:g}Z')


def export_vector(art, name, mono=False):
    # 96 design units correspond to the visible 72 dp of a 108 dp adaptive layer.
    vector = ET.Element('vector', {ANDROID+k: v for k,v in dict(width='108dp',height='108dp',
                        viewportWidth='144',viewportHeight='144').items()})
    group = ET.SubElement(vector, 'group', {ANDROID+'translateX':'24',ANDROID+'translateY':'24'})
    for layer in art.findall(SVG+'g'):
        for item in layer:
            attrs = {ANDROID+'fillColor': '#000000' if mono else layer.attrib['fill']}
            if item.get('fill-rule') == 'evenodd':
                attrs[ANDROID+'fillType'] = 'evenOdd'
            if item.tag == SVG+'path':
                data = ' '.join(item.attrib['d'].split())
            elif item.tag == SVG+'rect':
                data = rect_path(item)
            else:
                raise ValueError(item.tag)
            ET.SubElement(group, 'path', {**attrs, ANDROID+'pathData':data})
    ET.indent(vector, space='    ')
    output = RES / f'drawable/ic_launcher_{name}.xml'
    output.write_bytes(ET.tostring(vector, encoding='utf-8') + b'\n')


def raster(art, round_tile):
    art = deepcopy(art)
    if round_tile:
        art.find(SVG+'rect').set('rx','48')
    with pymupdf.open(stream=ET.tostring(art), filetype='svg') as doc:
        page = doc[0]
        scale = 1536 / page.rect.width
        pix = page.get_pixmap(matrix=pymupdf.Matrix(scale,scale), alpha=True)
        # PNG encoding unpremultiplies alpha before Pillow resamples the transparent edges.
        return Image.open(BytesIO(pix.tobytes('png'))).convert('RGBA')


def main():
    art = ET.parse(SOURCE).getroot()
    export_vector(art, 'foreground')
    export_vector(art, 'monochrome', mono=True)
    for rounded, suffix in ((False,''),(True,'_round')):
        image = raster(art, rounded)
        for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
            image.resize((size,size), Image.Resampling.LANCZOS).save(RES/f'mipmap-{density}/ic_launcher{suffix}.png')
        if not rounded:
            image.resize((512,512), Image.Resampling.LANCZOS).save(RES/'ic_launcher-web.png')


if __name__ == '__main__':
    main()
