/*
tip_balloon.js  v. 1.2

The latest version is available at
http://www.walterzorn.com
or http://www.devira.com
or http://www.walterzorn.de

Initial author: Walter Zorn
Last modified: 13.7.2007

Extension for the tooltip library wz_tooltip.js.
Implements balloon tooltips.
*/

// Here we define new global configuration variable(s) (as members of the
// predefined "config." class).
// From each of these config variables, wz_tooltip.js will automatically derive
// a command which can be passed to Tip() or TagToTip() in order to customize
// tooltips individually. These command names are just the config variable
// name(s) translated to uppercase,
// e.g. from config. Balloon a command BALLOON will automatically be
// created.

//===================  GLOBAL TOOPTIP CONFIGURATION  =========================//
config. Balloon = true				// true or false - set to true if you want this to be the default behaviour
// Sizes of balloon images
config. BalloonEdgeSize = 5			// Integer - sidelength of quadratic corner images
config. BalloonStemWidth = 15		// Integer
config. BalloonStemHeight = 19		// Integer
config. BalloonImgPath = 'js/tooltip/tip_balloon/'; // Path to images (border, corners, stem), in quotes. Path must be relative to your HTML file.
//=======  END OF TOOLTIP CONFIG, DO NOT CHANGE ANYTHING BELOW  ==============//


// Create a new tt_Extension object (make sure that the name of that object,
// here balloon, is unique amongst the extensions available for wz_tooltips.js):
var balloon = new tt_Extension();

// Implement extension eventhandlers on which our extension should react

balloon.OnLoadConfig = function()
{
	if(tt_aV[BALLOON])
	{
		// Turn off native style properties which are not appropriate
		balloon.padding = Math.max(tt_aV[PADDING] - tt_aV[BALLOONEDGESIZE], 0);
		balloon.width = tt_aV[WIDTH];
		//if(tt_bBoxOld)
		//	balloon.width += (balloon.padding << 1);
		tt_aV[BORDERWIDTH] = 0;
		tt_aV[WIDTH] = 0;
		tt_aV[PADDING] = 0;
		tt_aV[BGCOLOR] = "";
		tt_aV[BGIMG] = "";
		tt_aV[SHADOW] = false;
		// Append slash to img path if missing
		if(tt_aV[BALLOONIMGPATH].charAt(tt_aV[BALLOONIMGPATH].length - 1) != '/')
			tt_aV[BALLOONIMGPATH] += "/";
		return true;
	}
	return false;
};
balloon.OnCreateContentString = function()
{
	if(!tt_aV[BALLOON])
		return false;
		
	var aImg, sImgZ, sCssCrn, sCssImg;

	// Cache balloon images in advance:
	// Either use the pre-cached default images...
	if(tt_aV[BALLOONIMGPATH] == config.BalloonImgPath)
		aImg = balloon.aDefImg;
	// ...or load images from different directory
	else
		aImg = Balloon_CacheImgs(tt_aV[BALLOONIMGPATH]);
	sCssCrn = ' style="position:relative;width:' + tt_aV[BALLOONEDGESIZE] + 'px;padding:0px;margin:0px;overflow:hidden;line-height:0px;"';
	sCssImg = 'padding:0px;margin:0px;border:0px;';
	sImgZ = '" style="' + sCssImg + '" />';
	
	tt_sContent = '<table border="0" cellpadding="0" cellspacing="0" style="width:auto;padding:0px;margin:0px;left:0px;top:0px;"><tr>'
				// Left-top corner
				+ '<td' + sCssCrn + ' valign="bottom">'
				+ '<img src="' + aImg[1].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
				+ '</td>'
				// Top border
				+ '<td valign="bottom" style="position:relative;padding:0px;margin:0px;overflow:hidden;">'
				+ '<img id="bALlOOnT" style="position:relative;top:1px;z-index:1;display:none;' + sCssImg + '" src="' + aImg[9].src + '" width="' + tt_aV[BALLOONSTEMWIDTH] + '" height="' + tt_aV[BALLOONSTEMHEIGHT] + '" />'
				+ '<div style="position:relative;z-index:0;padding:0px;margin:0px;overflow:hidden;width:auto;height:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[2].src + ');">'
				+ '</div>'
				+ '</td>'
				// Right-top corner
				+ '<td' + sCssCrn + ' valign="bottom">'
				+ '<img src="' + aImg[3].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
				+ '</td>'
				+ '</tr><tr>'
				// Left border
				+ '<td style="position:relative;padding:0px;margin:0px;width:' + tt_aV[BALLOONEDGESIZE] + 'px;overflow:hidden;background-image:url(' + aImg[8].src + ');">'
				// Redundant image for bugous old Geckos that won't auto-expand TD height to 100%
				+ '<img width="' + tt_aV[BALLOONEDGESIZE] + '" height="100%" src="' + aImg[8].src + sImgZ
				+ '</td>'
				// Content
				+ '<td style="position:relative;line-height:normal;'
				+ ';background-image:url(' + aImg[0].src + ')'
				+ ';color:' + tt_aV[FONTCOLOR]
				+ ';font-family:' + tt_aV[FONTFACE]
				+ ';font-size:' + tt_aV[FONTSIZE]
				+ ';font-weight:' + tt_aV[FONTWEIGHT]
				+ ';text-align:' + tt_aV[TEXTALIGN]
				+ ';padding:' + balloon.padding
				+ ';width:' + (balloon.width ? (balloon.width + 'px') : 'auto')
				+ ';">' + tt_sContent + '</td>'
				// Right border
				+ '<td style="position:relative;padding:0px;margin:0px;width:' + tt_aV[BALLOONEDGESIZE] + 'px;overflow:hidden;background-image:url(' + aImg[4].src + ');">'
				// Image redundancy for bugous old Geckos that won't auto-expand TD height to 100%
				+ '<img width="' + tt_aV[BALLOONEDGESIZE] + '" height="100%" src="' + aImg[4].src + sImgZ
				+ '</td>'
				+ '</tr><tr>'
				// Left-bottom corner
				+ '<td valign="top"' + sCssCrn + '>'
				+ '<img src="' + aImg[7].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
				+ '</td>'
				// Bottom border
				+ '<td valign="top" style="position:relative;padding:0px;margin:0px;overflow:hidden;">'
				+ '<div style="position:relative;left:0px;top:0px;padding:0px;margin:0px;overflow:hidden;width:auto;height:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[6].src + ');"></div>'
				+ '<img id="bALlOOnB" style="position:relative;top:-1px;left:2px;z-index:1;display:none;' + sCssImg + '" src="' + aImg[10].src + '" width="' + tt_aV[BALLOONSTEMWIDTH] + '" height="' + tt_aV[BALLOONSTEMHEIGHT] + '" />'
				+ '</td>'
				// Right-bottom corner
				+ '<td valign="top"' + sCssCrn + '>'
				+ '<img src="' + aImg[5].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
				+ '</td>'
				+ '</tr></table>';
	return true;
};
balloon.OnSubDivsCreated = function()
{
	if(tt_aV[BALLOON])
	{
		balloon.iStem = tt_aV[ABOVE] * 1;
		balloon.stem = [tt_GetElt("bALlOOnT"), tt_GetElt("bALlOOnB")];
		balloon.stem[balloon.iStem].style.display = "inline";
		return true;
	}
	return false;
};
// Display the stem appropriately
balloon.OnMoveAfter = function()
{
	if(tt_aV[BALLOON])
	{
		var iStem = (tt_aV[ABOVE] != tt_bJmpVert) * 1;

		// Tooltip position vertically flipped?
		if(iStem != balloon.iStem)
		{
			// Display opposite stem
			balloon.stem[balloon.iStem].style.display = "none";
			balloon.stem[iStem].style.display = "inline";
			balloon.iStem = iStem;
		}
		
		balloon.stem[iStem].style.left = Balloon_CalcStemX() + "px";
		return true;
	}
	return false;
};
function Balloon_CalcStemX()
{
	var x = tt_musX - tt_x;
	return Math.max(Math.min(x, tt_w - tt_aV[BALLOONSTEMWIDTH] - (tt_aV[BALLOONEDGESIZE] << 1) - 2), 2);
}
function Balloon_CacheImgs(sPath)
{
	var asImg = ["background", "lt", "t", "rt", "r", "rb", "b", "lb", "l", "stemt", "stemb"],
	n = asImg.length,
	aImg = new Array(n),
	img;

	while(n)
	{--n;
		img = aImg[n] = new Image();
		img.src = sPath + asImg[n] + ".gif";
	}
	return aImg;
}
// This mechanism pre-caches the default images specified by
// congif.BalloonImgPath, so, whenever a balloon tip using these default images
// is created, no further server connection is necessary.
function Balloon_PreCacheDefImgs()
{
	// Append slash to img path if missing
	if(config.BalloonImgPath.charAt(config.BalloonImgPath.length - 1) != '/')
		config.BalloonImgPath += "/";
	// Preload default images into array
	balloon.aDefImg = Balloon_CacheImgs(config.BalloonImgPath);
}
Balloon_PreCacheDefImgs();
