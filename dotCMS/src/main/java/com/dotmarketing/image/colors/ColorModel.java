package com.dotmarketing.image.colors;

import io.vavr.Lazy;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class ColorModel {



    final static Lazy<List<DotColor>> colorMap = Lazy.of(() -> {

        // Web Named Colors:
        // https://htmlcolorcodes.com/color-names/
        List<DotColor> nm = new ArrayList<>();
        //     Color.decode("#RRGGBB

        nm.add(new DotColor(Color.decode("#CD5C5C"),"Red","IndianRed"));
        nm.add(new DotColor(Color.decode("#F08080"),"Red","LightCoral"));
        nm.add(new DotColor(Color.decode("#FA8072"),"Red","Salmon"));
        nm.add(new DotColor(Color.decode("#E9967A"),"Red","DarkSalmon"));
        nm.add(new DotColor(Color.decode("#FFA07A"),"Red","LightSalmon"));
        nm.add(new DotColor(Color.decode("#DC143C"),"Red","Crimson"));
        nm.add(new DotColor(Color.decode("#FF0000"),"Red","Red"));
        nm.add(new DotColor(Color.decode("#B22222"),"Red","FireBrick"));
        nm.add(new DotColor(Color.decode("#8B0000"),"Red","DarkRed"));
        nm.add(new DotColor(Color.decode("#FFC0CB"),"Pink","Pink"));
        nm.add(new DotColor(Color.decode("#FFB6C1"),"Pink","LightPink"));
        nm.add(new DotColor(Color.decode("#FF69B4"),"Pink","HotPink"));
        nm.add(new DotColor(Color.decode("#FF1493"),"Pink","DeepPink"));
        nm.add(new DotColor(Color.decode("#C71585"),"Pink","MediumVioletRed"));
        nm.add(new DotColor(Color.decode("#DB7093"),"Pink","PaleVioletRed"));
        nm.add(new DotColor(Color.decode("#FFA07A"),"Orange","LightSalmon"));
        nm.add(new DotColor(Color.decode("#FF7F50"),"Orange","Coral"));
        nm.add(new DotColor(Color.decode("#FF6347"),"Orange","Tomato"));
        nm.add(new DotColor(Color.decode("#FF4500"),"Orange","OrangeRed"));
        nm.add(new DotColor(Color.decode("#FF8C00"),"Orange","DarkOrange"));
        nm.add(new DotColor(Color.decode("#FFA500"),"Orange","Orange"));
        nm.add(new DotColor(Color.decode("#FFD700"),"Yellow","Gold"));
        nm.add(new DotColor(Color.decode("#FFFF00"),"Yellow","Yellow"));
        nm.add(new DotColor(Color.decode("#FFFFE0"),"Yellow","LightYellow"));
        nm.add(new DotColor(Color.decode("#FFFACD"),"Yellow","LemonChiffon"));
        nm.add(new DotColor(Color.decode("#FAFAD2"),"Yellow","LightGoldenrodYellow"));
        nm.add(new DotColor(Color.decode("#FFEFD5"),"Yellow","PapayaWhip"));
        nm.add(new DotColor(Color.decode("#FFE4B5"),"Yellow","Moccasin"));
        nm.add(new DotColor(Color.decode("#FFDAB9"),"Yellow","PeachPuff"));
        nm.add(new DotColor(Color.decode("#EEE8AA"),"Yellow","PaleGoldenrod"));
        nm.add(new DotColor(Color.decode("#F0E68C"),"Yellow","Khaki"));
        nm.add(new DotColor(Color.decode("#BDB76B"),"Yellow","DarkKhaki"));
        nm.add(new DotColor(Color.decode("#E6E6FA"),"Purple","Lavender"));
        nm.add(new DotColor(Color.decode("#D8BFD8"),"Purple","Thistle"));
        nm.add(new DotColor(Color.decode("#DDA0DD"),"Purple","Plum"));
        nm.add(new DotColor(Color.decode("#EE82EE"),"Purple","Violet"));
        nm.add(new DotColor(Color.decode("#DA70D6"),"Purple","Orchid"));
        nm.add(new DotColor(Color.decode("#FF00FF"),"Purple","Fuchsia"));
        nm.add(new DotColor(Color.decode("#FF00FF"),"Purple","Magenta"));
        nm.add(new DotColor(Color.decode("#BA55D3"),"Purple","MediumOrchid"));
        nm.add(new DotColor(Color.decode("#9370DB"),"Purple","MediumPurple"));
        nm.add(new DotColor(Color.decode("#663399"),"Purple","RebeccaPurple"));
        nm.add(new DotColor(Color.decode("#8A2BE2"),"Purple","BlueViolet"));
        nm.add(new DotColor(Color.decode("#9400D3"),"Purple","DarkViolet"));
        nm.add(new DotColor(Color.decode("#9932CC"),"Purple","DarkOrchid"));
        nm.add(new DotColor(Color.decode("#8B008B"),"Purple","DarkMagenta"));
        nm.add(new DotColor(Color.decode("#800080"),"Purple","Purple"));
        nm.add(new DotColor(Color.decode("#4B0082"),"Purple","Indigo"));
        nm.add(new DotColor(Color.decode("#6A5ACD"),"Purple","SlateBlue"));
        nm.add(new DotColor(Color.decode("#483D8B"),"Purple","DarkSlateBlue"));
        nm.add(new DotColor(Color.decode("#7B68EE"),"Purple","MediumSlateBlue"));
        nm.add(new DotColor(Color.decode("#ADFF2F"),"Green","GreenYellow"));
        nm.add(new DotColor(Color.decode("#7FFF00"),"Green","Chartreuse"));
        nm.add(new DotColor(Color.decode("#7CFC00"),"Green","LawnGreen"));
        nm.add(new DotColor(Color.decode("#00FF00"),"Green","Lime"));
        nm.add(new DotColor(Color.decode("#32CD32"),"Green","LimeGreen"));
        nm.add(new DotColor(Color.decode("#98FB98"),"Green","PaleGreen"));
        nm.add(new DotColor(Color.decode("#90EE90"),"Green","LightGreen"));
        nm.add(new DotColor(Color.decode("#00FA9A"),"Green","MediumSpringGreen"));
        nm.add(new DotColor(Color.decode("#00FF7F"),"Green","SpringGreen"));
        nm.add(new DotColor(Color.decode("#3CB371"),"Green","MediumSeaGreen"));
        nm.add(new DotColor(Color.decode("#2E8B57"),"Green","SeaGreen"));
        nm.add(new DotColor(Color.decode("#228B22"),"Green","ForestGreen"));
        nm.add(new DotColor(Color.decode("#008000"),"Green","Green"));
        nm.add(new DotColor(Color.decode("#006400"),"Green","DarkGreen"));
        nm.add(new DotColor(Color.decode("#9ACD32"),"Green","YellowGreen"));
        nm.add(new DotColor(Color.decode("#6B8E23"),"Green","OliveDrab"));
        nm.add(new DotColor(Color.decode("#808000"),"Green","Olive"));
        nm.add(new DotColor(Color.decode("#556B2F"),"Green","DarkOliveGreen"));
        nm.add(new DotColor(Color.decode("#66CDAA"),"Green","MediumAquamarine"));
        nm.add(new DotColor(Color.decode("#8FBC8B"),"Green","DarkSeaGreen"));
        nm.add(new DotColor(Color.decode("#20B2AA"),"Green","LightSeaGreen"));
        nm.add(new DotColor(Color.decode("#008B8B"),"Green","DarkCyan"));
        nm.add(new DotColor(Color.decode("#008080"),"Green","Teal"));
        nm.add(new DotColor(Color.decode("#00FFFF"),"Blue","Aqua"));
        nm.add(new DotColor(Color.decode("#00FFFF"),"Blue","Cyan"));
        nm.add(new DotColor(Color.decode("#E0FFFF"),"Blue","LightCyan"));
        nm.add(new DotColor(Color.decode("#AFEEEE"),"Blue","PaleTurquoise"));
        nm.add(new DotColor(Color.decode("#7FFFD4"),"Blue","Aquamarine"));
        nm.add(new DotColor(Color.decode("#40E0D0"),"Blue","Turquoise"));
        nm.add(new DotColor(Color.decode("#48D1CC"),"Blue","MediumTurquoise"));
        nm.add(new DotColor(Color.decode("#00CED1"),"Blue","DarkTurquoise"));
        nm.add(new DotColor(Color.decode("#5F9EA0"),"Blue","CadetBlue"));
        nm.add(new DotColor(Color.decode("#4682B4"),"Blue","SteelBlue"));
        nm.add(new DotColor(Color.decode("#B0C4DE"),"Blue","LightSteelBlue"));
        nm.add(new DotColor(Color.decode("#B0E0E6"),"Blue","PowderBlue"));
        nm.add(new DotColor(Color.decode("#ADD8E6"),"Blue","LightBlue"));
        nm.add(new DotColor(Color.decode("#87CEEB"),"Blue","SkyBlue"));
        nm.add(new DotColor(Color.decode("#87CEFA"),"Blue","LightSkyBlue"));
        nm.add(new DotColor(Color.decode("#00BFFF"),"Blue","DeepSkyBlue"));
        nm.add(new DotColor(Color.decode("#1E90FF"),"Blue","DodgerBlue"));
        nm.add(new DotColor(Color.decode("#6495ED"),"Blue","CornflowerBlue"));
        nm.add(new DotColor(Color.decode("#7B68EE"),"Blue","MediumSlateBlue"));
        nm.add(new DotColor(Color.decode("#4169E1"),"Blue","RoyalBlue"));
        nm.add(new DotColor(Color.decode("#0000FF"),"Blue","Blue"));
        nm.add(new DotColor(Color.decode("#0000CD"),"Blue","MediumBlue"));
        nm.add(new DotColor(Color.decode("#00008B"),"Blue","DarkBlue"));
        nm.add(new DotColor(Color.decode("#000080"),"Blue","Navy"));
        nm.add(new DotColor(Color.decode("#191970"),"Blue","MidnightBlue"));
        nm.add(new DotColor(Color.decode("#FFF8DC"),"Brown","Cornsilk"));
        nm.add(new DotColor(Color.decode("#FFEBCD"),"Brown","BlanchedAlmond"));
        nm.add(new DotColor(Color.decode("#FFE4C4"),"Brown","Bisque"));
        nm.add(new DotColor(Color.decode("#FFDEAD"),"Brown","NavajoWhite"));
        nm.add(new DotColor(Color.decode("#F5DEB3"),"Brown","Wheat"));
        nm.add(new DotColor(Color.decode("#DEB887"),"Brown","BurlyWood"));
        nm.add(new DotColor(Color.decode("#D2B48C"),"Brown","Tan"));
        nm.add(new DotColor(Color.decode("#BC8F8F"),"Brown","RosyBrown"));
        nm.add(new DotColor(Color.decode("#F4A460"),"Brown","SandyBrown"));
        nm.add(new DotColor(Color.decode("#DAA520"),"Brown","Goldenrod"));
        nm.add(new DotColor(Color.decode("#B8860B"),"Brown","DarkGoldenrod"));
        nm.add(new DotColor(Color.decode("#CD853F"),"Brown","Peru"));
        nm.add(new DotColor(Color.decode("#D2691E"),"Brown","Chocolate"));
        nm.add(new DotColor(Color.decode("#8B4513"),"Brown","SaddleBrown"));
        nm.add(new DotColor(Color.decode("#A0522D"),"Brown","Sienna"));
        nm.add(new DotColor(Color.decode("#A52A2A"),"Brown","Brown"));
        nm.add(new DotColor(Color.decode("#800000"),"Brown","Maroon"));
        nm.add(new DotColor(Color.decode("#FFFFFF"),"White","White"));
        nm.add(new DotColor(Color.decode("#FFFAFA"),"White","Snow"));
        nm.add(new DotColor(Color.decode("#F0FFF0"),"White","HoneyDew"));
        nm.add(new DotColor(Color.decode("#F5FFFA"),"White","MintCream"));
        nm.add(new DotColor(Color.decode("#F0FFFF"),"White","Azure"));
        nm.add(new DotColor(Color.decode("#F0F8FF"),"White","AliceBlue"));
        nm.add(new DotColor(Color.decode("#F8F8FF"),"White","GhostWhite"));
        nm.add(new DotColor(Color.decode("#F5F5F5"),"White","WhiteSmoke"));
        nm.add(new DotColor(Color.decode("#FFF5EE"),"White","SeaShell"));
        nm.add(new DotColor(Color.decode("#F5F5DC"),"White","Beige"));
        nm.add(new DotColor(Color.decode("#FDF5E6"),"White","OldLace"));
        nm.add(new DotColor(Color.decode("#FFFAF0"),"White","FloralWhite"));
        nm.add(new DotColor(Color.decode("#FFFFF0"),"White","Ivory"));
        nm.add(new DotColor(Color.decode("#FAEBD7"),"White","AntiqueWhite"));
        nm.add(new DotColor(Color.decode("#FAF0E6"),"White","Linen"));
        nm.add(new DotColor(Color.decode("#FFF0F5"),"White","LavenderBlush"));
        nm.add(new DotColor(Color.decode("#FFE4E1"),"White","MistyRose"));
        nm.add(new DotColor(Color.decode("#DCDCDC"),"Gray","Gainsboro"));
        nm.add(new DotColor(Color.decode("#D3D3D3"),"Gray","LightGray"));
        nm.add(new DotColor(Color.decode("#C0C0C0"),"Gray","Silver"));
        nm.add(new DotColor(Color.decode("#A9A9A9"),"Gray","DarkGray"));
        nm.add(new DotColor(Color.decode("#808080"),"Gray","Gray"));
        nm.add(new DotColor(Color.decode("#696969"),"Gray","DimGray"));
        nm.add(new DotColor(Color.decode("#778899"),"Gray","LightSlateGray"));
        nm.add(new DotColor(Color.decode("#708090"),"Gray","SlateGray"));
        nm.add(new DotColor(Color.decode("#2F4F4F"),"Gray","DarkSlateGray"));
        nm.add(new DotColor(Color.decode("#000000"),"Gray","Black"));



        return nm;
    });



    static Lazy<IndexColorModel> colorModel = Lazy.of(() -> {
        final int[] cmap =  colorMap.get().stream().mapToInt(k->k.getRGB()).toArray();
        final int bits = (int) Math.ceil(Math.log(cmap.length) / Math.log(2));
        return new IndexColorModel(bits, cmap.length, cmap, 0, false, 3, DataBuffer.TYPE_BYTE);
    });

    final static DotColor black = new DotColor(Color.decode("#000000"),"Gray","Black");

    public static DotColor nearestColor(@NotNull Color color) {
        final int index = ((byte[])ColorModel.colorModel.get().getDataElements(color.getRGB(), null))[0];
        return  Try.of (()->colorMap.get().get(index)).getOrElse(black);

    }





}
