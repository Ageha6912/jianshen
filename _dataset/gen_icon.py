# -*- coding: utf-8 -*-
# 生成「健身打卡」启动图标:暖黑底 + 白色哑铃 + 白金对勾徽章(v3 白金风)
from PIL import Image, ImageDraw
import os

RES = 'E:/jianshen/FitnessApp/app/src/main/res'

WARM_BLACK = (55, 53, 47, 255)      # #37352F
PLATINUM = (232, 225, 213, 255)     # #E8E1D5 香槟铂金(v3 品牌色)
WHITE = (255, 255, 255, 255)
TRANS = (0, 0, 0, 0)

S = 1024

# 哑铃几何(画布 1024,哑铃中心 (512, 480))
PLATE_OUTER_L = (222, 320, 322, 640)   # 左外片
PLATE_INNER_L = (322, 370, 382, 590)   # 左内片
BAR = (382, 462, 642, 498)             # 杆(胶囊)
PLATE_INNER_R = (642, 370, 702, 590)
PLATE_OUTER_R = (702, 320, 802, 640)
# 徽章(右下)
BADGE_C = (742, 742)
BADGE_R = 130
CHECK = [(682, 748), (726, 794), (806, 690)]
CHECK_W = 36

# 整体构图包围盒(哑铃 ∪ 徽章)
COMP_BBOX = (222, 320, 872, 872)
COMP_CENTER = ((COMP_BBOX[0] + COMP_BBOX[2]) // 2, (COMP_BBOX[1] + COMP_BBOX[3]) // 2)


def draw_glyph():
    """在 1024 透明画布上画哑铃+徽章,构图中心记入 COMP_CENTER。"""
    img = Image.new('RGBA', (S, S), TRANS)
    d = ImageDraw.Draw(img)
    rrect = lambda box, r: d.rounded_rectangle(box, radius=r, fill=WHITE)
    rrect(PLATE_OUTER_L, 30)
    rrect(PLATE_OUTER_R, 30)
    rrect(PLATE_INNER_L, 22)
    rrect(PLATE_INNER_R, 22)
    rrect(BAR, 18)
    # 徽章
    d.ellipse((BADGE_C[0] - BADGE_R, BADGE_C[1] - BADGE_R,
               BADGE_C[0] + BADGE_R, BADGE_C[1] + BADGE_R), fill=PLATINUM)
    d.line(CHECK, fill=WARM_BLACK, width=CHECK_W, joint='curve')
    for p in (CHECK[0], CHECK[2]):  # 圆头端点
        d.ellipse((p[0] - CHECK_W // 2, p[1] - CHECK_W // 2,
                   p[0] + CHECK_W // 2, p[1] + CHECK_W // 2), fill=WARM_BLACK)
    return img


GLYPH = draw_glyph()


def paste_centered(base, img, scale, center):
    """把 img 缩放 scale 后,以其构图中心对齐到 base 的 center 处粘贴。"""
    n = int(S * scale)
    small = img.resize((n, n), Image.LANCZOS)
    off = (int(center[0] - COMP_CENTER[0] * scale), int(center[1] - COMP_CENTER[1] * scale))
    base.alpha_composite(small, dest=off)


def legacy_square():
    img = Image.new('RGBA', (S, S), TRANS)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, S - 1, S - 1), radius=185, fill=WARM_BLACK)
    paste_centered(img, GLYPH, 0.94, (512, 512))
    return img


def legacy_round():
    img = Image.new('RGBA', (S, S), TRANS)
    d = ImageDraw.Draw(img)
    d.ellipse((0, 0, S - 1, S - 1), fill=WARM_BLACK)
    paste_centered(img, GLYPH, 0.84, (512, 512))
    return img


def foreground():
    """自适应图标前景:构图缩入安全区(66/108 ≈ 半径 313)。"""
    img = Image.new('RGBA', (S, S), TRANS)
    # 构图外接圆半径 426(角点到构图中心),0.70 倍后 ≈ 298 < 313
    paste_centered(img, GLYPH, 0.70, (512, 512))
    return img


LAUNCHER = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
FG = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}

sq, rd, fg = legacy_square(), legacy_round(), foreground()

for dpi, size in LAUNCHER.items():
    d = f'{RES}/mipmap-{dpi}'
    os.makedirs(d, exist_ok=True)
    sq.resize((size, size), Image.LANCZOS).save(f'{d}/ic_launcher.png')
    rd.resize((size, size), Image.LANCZOS).save(f'{d}/ic_launcher_round.png')

for dpi, size in FG.items():
    d = f'{RES}/mipmap-{dpi}'
    os.makedirs(d, exist_ok=True)
    fg.resize((size, size), Image.LANCZOS).save(f'{d}/ic_launcher_foreground.png')

# 预览图(给用户看)
preview = Image.new('RGBA', (512, 260), TRANS)
preview.alpha_composite(sq.resize((240, 240), Image.LANCZOS), (4, 8))
preview.alpha_composite(rd.resize((240, 240), Image.LANCZOS), (268, 8))
preview.save('E:/jianshen/_dataset/icon_preview.png')
print('icons generated')
