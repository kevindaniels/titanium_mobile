/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIiOSProxy.h"
#import "TiUtils.h"
#import "Webcolor.h"
#ifdef USE_TI_UIIOS

#ifdef USE_TI_UIIOSPREVIEWCONTEXT
#import "TiUIiOSPreviewContextProxy.h"
#import "TiUIiOSPreviewActionProxy.h"
#import "TiUIiOSPreviewActionGroupProxy.h"
#endif

#ifdef USE_TI_UIIOSMENUPOPUP
#import "TiUIiOSMenuPopupProxy.h"
#endif

#if IS_XCODE_7_1
#ifdef USE_TI_UIIOSLIVEPHOTOVIEW
#import "TiUIiOSLivePhotoViewProxy.h"
#endif
#endif

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
#import "TiUIiOSTransitionAnimationProxy.h"
#endif

#ifdef USE_TI_UIIOSADVIEW
	#import "TiUIiOSAdViewProxy.h"
    #import <iAd/iAd.h>
#endif

#ifdef USE_TI_UIIOS3DMATRIX
	#import "Ti3DMatrix.h"
#endif

#ifdef USE_TI_UIIOSCOVERFLOWVIEW
	#import "TiUIiOSCoverFlowViewProxy.h"
#endif
#ifdef USE_TI_UIIOSTOOLBAR
	#import "TiUIiOSToolbarProxy.h"
#endif
#ifdef USE_TI_UIIOSTABBEDBAR
	#import "TiUIiOSTabbedBarProxy.h"
#endif

#if defined(USE_TI_UIIPADDOCUMENTVIEWER) || defined(USE_TI_UIIOSDOCUMENTVIEWER)
    #import "TiUIiOSDocumentViewerProxy.h"
#endif
#ifdef USE_TI_UIIOSACTIVITYVIEW
#import "TiUIiOSActivityViewProxy.h"
#endif
#ifdef USE_TI_UIIOSACTIVITY
#import "TiUIiOSActivityProxy.h"
#endif
#ifdef USE_TI_UIIOSSPLITWINDOW
#import "TiUIiOSSplitWindowProxy.h"
#endif

#ifdef USE_TI_UIIOSANIMATOR
#import "TiAnimatorProxy.h"
#ifdef USE_TI_UIIOSSNAPBEHAVIOR
#import "TiSnapBehavior.h"
#endif
#ifdef USE_TI_UIIOSPUSHBEHAVIOR
#import "TiPushBehavior.h"
#endif
#ifdef USE_TI_UIIOSGRAVITYBEHAVIOR
#import "TiGravityBehavior.h"
#endif
#ifdef USE_TI_UIIOSANCHORATTACHMENTBEHAVIOR
#import "TiAnchorAttachBehavior.h"
#endif
#ifdef USE_TI_UIIOSVIEWATTACHMENTBEHAVIOR
#import "TiViewAttachBehavior.h"
#endif
#ifdef USE_TI_UIIOSCOLLISIONBEHAVIOR
#import "TiCollisionBehavior.h"
#endif
#ifdef USE_TI_UIIOSDYNAMICITEMBEHAVIOR
#import "TiDynamicItemBehavior.h"
#endif
#endif
#ifdef USE_TI_UIIOSAPPLICATIONSHORTCUTS
#import "TiUIiOSApplicationShortcutsProxy.h"
#endif
#if IS_XCODE_7_1
#if defined(USE_TI_UIIOSLIVEPHOTOBADGE) || defined(USE_TI_UIIOSLIVEPHOTOVIEW)
#import <PhotosUI/PhotosUI.h>
#endif
#endif
#ifdef USE_TI_UIIOSBLURVIEW
#import "TiUIiOSBlurViewProxy.h"
#endif

@implementation TiUIiOSProxy

#define FORGET_AND_RELEASE(x) \
{\
[self forgetProxy:x]; \
RELEASE_TO_NIL(x); \
}


-(NSString*)apiName
{
    return @"Ti.UI.iOS";
}

-(NSNumber*)forceTouchSupported
{
    return NUMBOOL([TiUtils forceTouchSupported]);
}

-(NSNumber*)SCROLL_DECELERATION_RATE_NORMAL
{
    return NUMFLOAT(UIScrollViewDecelerationRateNormal);
}

-(NSNumber*)SCROLL_DECELERATION_RATE_FAST
{
    return NUMFLOAT(UIScrollViewDecelerationRateFast);
}

-(NSNumber*)CLIP_MODE_DEFAULT
{
    return NUMINT(0);
}
-(NSNumber*)CLIP_MODE_ENABLED
{
    return NUMINT(1);
}
-(NSNumber*)CLIP_MODE_DISABLED
{
    return NUMINT(-1);
}

#ifdef USE_TI_UILISTVIEW
-(NSNumber*) ROW_ACTION_STYLE_DEFAULT
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleDefault);
    }
    return nil;
}
-(NSNumber*) ROW_ACTION_STYLE_DESTRUCTIVE
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleDestructive);
    }
    return nil;
}
-(NSNumber*) ROW_ACTION_STYLE_NORMAL
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleNormal);
    }
    return nil;
}
#endif

#ifdef USE_TI_UIIOSPREVIEWCONTEXT
-(NSNumber*) PREVIEW_ACTION_STYLE_DEFAULT
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleDefault);
    }
#endif
    return nil;
}
-(NSNumber*) PREVIEW_ACTION_STYLE_DESTRUCTIVE
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleDestructive);
    }
#endif
    return nil;
}
-(NSNumber*) PREVIEW_ACTION_STYLE_SELECTED
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleSelected);
    }
#endif
    return nil;
}
#endif


- (void)dealloc
{
#ifdef USE_TI_UIIPHONEANIMATIONSTYLE
    RELEASE_TO_NIL(_animationStyleProxy);
#endif
#ifdef USE_TI_UIIOSROWANIMATIONSTYLE
    RELEASE_TO_NIL(_RowAnimationStyle);
#endif
#ifdef USE_TI_UIIOSALERTDIALOGSTYLE
    RELEASE_TO_NIL(_AlertDialogStyle);
#endif
#if defined(USE_TI_UIIOSTABLEVIEWCELLSELECTIONSTYLE) || defined (USE_TI_UIIOSLISTVIEWCELLSELECTIONSTYLE)
    RELEASE_TO_NIL(_TableViewCellSelectionStyle);
    RELEASE_TO_NIL(_ListViewCellSelectionStyle);
#endif
#if defined(USE_TI_UIIOSTABLEVIEWSCROLLPOSITION) || defined(USE_TI_UIIOSLISTVIEWSCROLLPOSITION)
    RELEASE_TO_NIL(_TableViewScrollPosition);
    RELEASE_TO_NIL(_ListViewScrollPosition);
#endif
#if defined(USE_TI_UIIOSTABLEVIEWSTYLE) || defined(USE_TI_UIIOSLISTVIEWSTYLE)
    RELEASE_TO_NIL(_TableViewStyle);
    RELEASE_TO_NIL(_ListViewStyle);
#endif
#ifdef USE_TI_UIIOSPROGRESSBARSTYLE
    RELEASE_TO_NIL(_ProgressBarStyle);
#endif
#ifdef USE_TI_UIIOSSCROLLINDICATORSTYLE
    RELEASE_TO_NIL(_ScrollIndicatorStyle);
#endif
#ifdef USE_TI_UIIOSSTATUSBAR
    RELEASE_TO_NIL(_StatusBar);
#endif
#ifdef USE_TI_UIIOSSYSTEMBUTTONSTYLE
    RELEASE_TO_NIL(_SystemButton);
#endif
#ifdef USE_TI_UIIOSSYSTEMBUTTONSTYLE
    RELEASE_TO_NIL(_SystemButtonStyle);
#endif
#ifdef USE_TI_UIIOSSYSTEMICON
    RELEASE_TO_NIL(_SystemIcon);
#endif

    [super dealloc];
}

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
#ifdef USE_TI_UIIPHONEANIMATIONSTYLE
    FORGET_AND_RELEASE(_animationStyleProxy);
#endif

#ifdef USE_TI_UIIOSROWANIMATIONSTYLE
    FORGET_AND_RELEASE(_RowAnimationStyle);
#endif
#ifdef USE_TI_UIIOSALERTDIALOGSTYLE
    FORGET_AND_RELEASE(_AlertDialogStyle);
#endif
#if defined(USE_TI_UIIOSTABLEVIEWCELLSELECTIONSTYLE) || defined (USE_TI_UIIOSLISTVIEWCELLSELECTIONSTYLE)
    FORGET_AND_RELEASE(_TableViewCellSelectionStyle);
    FORGET_AND_RELEASE(_ListViewCellSelectionStyle);
    
#endif
    
#if defined(USE_TI_UIIOSTABLEVIEWSCROLLPOSITION) || defined(USE_TI_UIIOSLISTVIEWSCROLLPOSITION)
    FORGET_AND_RELEASE(_TableViewScrollPosition);
    FORGET_AND_RELEASE(_ListViewScrollPosition);
#endif
#if defined(USE_TI_UIIOSTABLEVIEWSTYLE) || defined(USE_TI_UIIOSLISTVIEWSTYLE)
    FORGET_AND_RELEASE(_TableViewStyle);
    FORGET_AND_RELEASE(_ListViewStyle);
#endif
    
#ifdef USE_TI_UIIOSPROGRESSBARSTYLE
    FORGET_AND_RELEASE(_ProgressBarStyle);
#endif
#ifdef USE_TI_UIIOSSCROLLINDICATORSTYLE
    FORGET_AND_RELEASE(_ScrollIndicatorStyle);
#endif
#ifdef USE_TI_UIIOSSTATUSBAR
    FORGET_AND_RELEASE(_StatusBar);
#endif
#ifdef USE_TI_UIIOSSYSTEMBUTTONSTYLE
    FORGET_AND_RELEASE(_SystemButtonStyle);
#endif
    
#ifdef USE_TI_UIIOSSYSTEMBUTTON
  FORGET_AND_RELEASE(_SystemButton);
#endif
#ifdef USE_TI_UIIOSSYSTEMICON
    FORGET_AND_RELEASE(_SystemIcon);
#endif
    [super didReceiveMemoryWarning:notification];
}

#ifdef USE_TI_UIIOSALERTDIALOGSTYLE
-(TIUIiOSAlertDialogStyleProxy*)AlertDialogStyle
{
    if (_AlertDialogStyle == nil) {
       _AlertDialogStyle = [[TIUIiOSAlertDialogStyleProxy alloc] _initWithPageContext:[self pageContext]];
    }
    return _AlertDialogStyle;
}
#endif

#ifdef USE_TI_UIIOSANIMATIONSTYLE
-(TiUIiOSAnimationStyleProxy*)AnimationStyle
{
    if (_animationStyleProxy == nil) {
        _animationStyleProxy = [[TiUIiOSAnimationStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _animationStyleProxy;
}
#endif


#if defined(USE_TI_UIIOSTABLEVIEWCELLSELECTIONSTYLE) || defined (USE_TI_UIIOSLISTVIEWCELLSELECTIONSTYLE)
-(TiUIiOSTableViewCellSelectionStyleProxy*)TableViewCellSelectionStyle
{
    if (_TableViewCellSelectionStyle == nil) {
        _TableViewCellSelectionStyle = [[TiUIiOSTableViewCellSelectionStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _TableViewCellSelectionStyle;
}

-(TiUIiOSTableViewCellSelectionStyleProxy*)ListViewCellSelectionStyle
{
    if (_ListViewCellSelectionStyle == nil) {
        _ListViewCellSelectionStyle = [[TiUIiOSTableViewCellSelectionStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _ListViewCellSelectionStyle;
}
#endif

#if defined(USE_TI_UIIOSTABLEVIEWSCROLLPOSITION) || defined(USE_TI_UIIOSLISTVIEWSCROLLPOSITION)
-(TiUIiOSTableViewScrollPositionProxy*)TableViewScrollPosition
{
    if (_TableViewScrollPosition == nil) {
        _TableViewScrollPosition = [[TiUIiOSTableViewScrollPositionProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _TableViewScrollPosition;
}

-(TiUIiOSTableViewScrollPositionProxy*)ListViewScrollPosition
{
    if (_ListViewScrollPosition == nil) {
        _ListViewScrollPosition = [[TiUIiOSTableViewScrollPositionProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _ListViewScrollPosition;
}
#endif

#if defined(USE_TI_UIIOSTABLEVIEWSTYLE) || defined(USE_TI_UIIOSLISTVIEWSTYLE)
-(TiUIiOSTableViewStyleProxy*)TableViewStyle
{
    if (_TableViewStyle == nil) {
    _TableViewStyle = [[TiUIiOSTableViewStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _TableViewStyle;
}

-(TiUIiOSTableViewStyleProxy*)ListViewStyle
{
    if (_ListViewStyle == nil) {
        _ListViewStyle = [[TiUIiOSTableViewStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _ListViewStyle;
}
#endif

#ifdef USE_TI_UIIOSANIMATIONSTYLE
-(TiUIiOSProgressBarStyleProxy*)ProgressBarStyle
{
    if (_ProgressBarStyle == nil) {
        _ProgressBarStyle = [[TiUIiOSProgressBarStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _ProgressBarStyle;
}
#endif

#ifdef USE_TI_UIIOSROWANIMATIONSTYLE
-(TiUIiOSRowAnimationStyleProxy*)RowAnimationStyle
{
    if (_RowAnimationStyle == nil) {
        _RowAnimationStyle = [[TiUIiOSRowAnimationStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _RowAnimationStyle;
}
#endif

#ifdef USE_TI_UIIOSSCROLLINDICATORSTYLE
-(TiUIiOSScrollIndicatorStyleProxy*)ScrollIndicatorStyle
{
    if (_ScrollIndicatorStyle == nil) {
        _ScrollIndicatorStyle = [[TiUIiOSScrollIndicatorStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _ScrollIndicatorStyle;
}
#endif

#ifdef USE_TI_UIIOSSTATUSBAR
-(TiUIiOSStatusBarProxy*)StatusBar
{
    if (_StatusBar == nil) {
         _StatusBar = [[TiUIiOSStatusBarProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _StatusBar;
}
#endif

#ifdef USE_TI_UIIOSSYSTEMBUTTONSTYLE
-(TiUIiOSSystemButtonStyleProxy*)SystemButtonStyle
{
    if (_SystemButtonStyle == nil) {
        _SystemButtonStyle = [[TiUIiOSSystemButtonStyleProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _SystemButtonStyle;
}
#endif

#ifdef USE_TI_UIIOSSYSTEMBUTTON
-(TiUIiOSSystemButtonProxy*)SystemButton
{
    if (_SystemButton == nil) {
        _SystemButton = [[TiUIiOSSystemButtonProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _SystemButton;
}
#endif

#ifdef USE_TI_UIIOSSYSTEMICON
-(TiUIiOSSystemIconProxy*)SystemIcon
{
    if (_SystemIcon == nil) {
        _SystemIcon = [[TiUIiOSSystemIconProxy alloc]_initWithPageContext:[self pageContext]];
    }
    return _SystemIcon;
}
#endif

-(void)setAppBadge:(id)value
{
    ENSURE_UI_THREAD(setAppBadge,value);
    if (value == [NSNull null]) {
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:0];
    } else {
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:[TiUtils intValue:value]];
    }
}

BEGIN_UI_THREAD_PROTECTED_VALUE(appBadge,NSNumber)
result = [NSNumber numberWithInteger:[[UIApplication sharedApplication] applicationIconBadgeNumber]];
END_UI_THREAD_PROTECTED_VALUE(appBadge)

-(void)setAppSupportsShakeToEdit:(NSNumber *)shake
{
    ENSURE_UI_THREAD(setAppSupportsShakeToEdit,shake);
    [[UIApplication sharedApplication] setApplicationSupportsShakeToEdit:[shake boolValue]];
}

BEGIN_UI_THREAD_PROTECTED_VALUE(appSupportsShakeToEdit,NSNumber)
result = [NSNumber numberWithBool:[[UIApplication sharedApplication] applicationSupportsShakeToEdit]];
END_UI_THREAD_PROTECTED_VALUE(appSupportsShakeToEdit)

#ifdef USE_TI_UIIOSBLURVIEW
- (NSNumber*) BLUR_EFFECT_STYLE_EXTRA_LIGHT
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UIBlurEffectStyleExtraLight);
    }
    return nil;
}

- (NSNumber* )BLUR_EFFECT_STYLE_LIGHT
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UIBlurEffectStyleLight);
    }
    return nil;
}

- (NSNumber*) BLUR_EFFECT_STYLE_DARK
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UIBlurEffectStyleDark);
    }
    return nil;
}
#endif

#ifdef USE_TI_UIIOSMENUPOPUP
MAKE_SYSTEM_PROP(MENU_POPUP_ARROW_DIRECTION_UP, UIMenuControllerArrowUp);
MAKE_SYSTEM_PROP(MENU_POPUP_ARROW_DIRECTION_DOWN, UIMenuControllerArrowDown);
MAKE_SYSTEM_PROP(MENU_POPUP_ARROW_DIRECTION_LEFT, UIMenuControllerArrowLeft);
MAKE_SYSTEM_PROP(MENU_POPUP_ARROW_DIRECTION_RIGHT, UIMenuControllerArrowRight);
MAKE_SYSTEM_PROP(MENU_POPUP_ARROW_DIRECTION_DEFAULT, UIMenuControllerArrowDefault);
#endif

#ifdef USE_TI_UIIOSADVIEW

-(NSString*)AD_SIZE_PORTRAIT 
{
    return [TiUIiOSAdViewProxy portraitSize];
}

-(NSString*)AD_SIZE_LANDSCAPE 
{
    return [TiUIiOSAdViewProxy landscapeSize];
}

-(id)createAdView:(id)args
{
	return [[[TiUIiOSAdViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

#endif

#ifdef USE_TI_UIIOS3DMATRIX
-(id)create3DMatrix:(id)args
{
	DEPRECATED_REPLACED_REMOVED(@"UI.iOS.create3DMatrix()", @"2.1.0", @"6.0.0", @"UI.create3DMatrix()");
    if (args==nil || [args count] == 0)
	{
		return [[[Ti3DMatrix alloc] init] autorelease];
	}
	ENSURE_SINGLE_ARG(args,NSDictionary);
	Ti3DMatrix *matrix = [[Ti3DMatrix alloc] initWithProperties:args];
	return [matrix autorelease];
}
#endif
#ifdef USE_TI_UIIOSCOVERFLOWVIEW
-(id)createCoverFlowView:(id)args
{
	return [[[TiUIiOSCoverFlowViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSTOOLBAR
-(id)createToolbar:(id)args
{
	return [[[TiUIiOSToolbarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSTABBEDBAR
-(id)createTabbedBar:(id)args
{
    return [[[TiUIiOSTabbedBarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#if defined(USE_TI_UIIPADDOCUMENTVIEWER) || defined(USE_TI_UIIOSDOCUMENTVIEWER)
-(id)createDocumentViewer:(id)args
{
	return [[[TiUIiOSDocumentViewerProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSACTIVITYVIEW
-(id)createActivityView:(id)args
{
    return [[[TiUIiOSActivityViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSACTIVITY
-(id)createActivity:(id)args
{
    return [[[TiUIiOSActivityProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSSPLITWINDOW
-(id)createSplitWindow:(id)args
{
    return [[[TiUIiOSSplitWindowProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
-(id)createTransitionAnimation:(id)args;
{
    return [[[TiUIiOSTransitionAnimationProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#if IS_XCODE_7
#ifdef USE_TI_UIIOSPREVIEWCONTEXT
-(id)createPreviewAction:(id)args
{
    return [[[TiUIiOSPreviewActionProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

-(id)createPreviewActionGroup:(id)args
{
    return [[[TiUIiOSPreviewActionGroupProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

-(id)createPreviewContext:(id)args
{
    return [[[TiUIiOSPreviewContextProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#endif

#ifdef USE_TI_UIIOSMENUPOPUP
-(id)createMenuPopup:(id)args
{
    return [[[TiUIiOSMenuPopupProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSBLURVIEW
-(id)createBlurView:(id)args
{
    return [[[TiUIiOSBlurViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#if IS_XCODE_7_1
#ifdef USE_TI_UIIOSLIVEPHOTOVIEW
-(id)createLivePhotoView:(id)args
{
    return [[[TiUIiOSLivePhotoViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

-(NSNumber*) LIVEPHOTO_PLAYBACK_STYLE_FULL
{
    if ([TiUtils isIOS9_1OrGreater]) {
        return NUMINTEGER(PHLivePhotoViewPlaybackStyleFull);
    }
    return nil;
}

-(NSNumber*) LIVEPHOTO_PLAYBACK_STYLE_HINT
{
    if ([TiUtils isIOS9_1OrGreater]) {
        return NUMINTEGER(PHLivePhotoViewPlaybackStyleHint);
    }
    
    return nil;
}
#endif

#ifdef USE_TI_UIIOSLIVEPHOTOBADGE
-(TiBlob*)createLivePhotoBadge:(id)value
{
    if ([TiUtils isIOS9_1OrGreater] == NO) {
        return nil;
    }
    
    ENSURE_ARG_COUNT(value, 1);
    ENSURE_ARRAY(value);
    id option = [value objectAtIndex:0];
    
    UIImage *badge = [PHLivePhotoView livePhotoBadgeImageWithOptions:[TiUtils intValue:option def:PHLivePhotoBadgeOptionsOverContent]];
    
    // Badges only work on devices.
    if (badge == nil) {
        return nil;
    }
    
    TiBlob *image = [[TiBlob alloc] initWithImage:badge];
    
    return image;
}
#endif

#ifdef USE_TI_UIIOSLIVEPHOTO_BADGE_OPTIONS_OVER_CONTENT
-(NSNumber*)LIVEPHOTO_BADGE_OPTIONS_OVER_CONTENT
{
    if ([TiUtils isIOS9_1OrGreater]) {
        return NUMINTEGER(PHLivePhotoBadgeOptionsOverContent);
    }
    return NUMINT(0);
}
#endif

#ifdef USE_TI_UIIOSLIVEPHOTO_BADGE_OPTIONS_LIVE_OFF
-(NSNumber*)LIVEPHOTO_BADGE_OPTIONS_LIVE_OFF
{
    if ([TiUtils isIOS9_1OrGreater]) {
        return NUMINTEGER(PHLivePhotoBadgeOptionsLiveOff);
    }
    return NUMINT(0);
}
#endif

#endif

#ifdef USE_TI_UIIOSANIMATOR
-(id)createAnimator:(id)args
{
    return [[[TiAnimatorProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#ifdef USE_TI_UIIOSSNAPBEHAVIOR
-(id)createSnapBehavior:(id)args
{
    return [[[TiSnapBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSPUSHBEHAVIOR
-(id)createPushBehavior:(id)args
{
    return [[[TiPushBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
//TiPushBehavior Constants
MAKE_SYSTEM_PROP(PUSH_MODE_CONTINUOUS, 0);
MAKE_SYSTEM_PROP(PUSH_MODE_INSTANTANEOUS, 1);
#endif

#ifdef USE_TI_UIIOSGRAVITYBEHAVIOR
-(id)createGravityBehavior:(id)args
{
    return [[[TiGravityBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSANCHORATTACHMENTBEHAVIOR
-(id)createAnchorAttachmentBehavior:(id)args
{
    return [[[TiAnchorAttachBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSVIEWATTACHMENTBEHAVIOR
-(id)createViewAttachmentBehavior:(id)args
{
    return [[[TiViewAttachBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSCOLLISIONBEHAVIOR
-(id)createCollisionBehavior:(id)args
{
    return [[[TiCollisionBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
//TiCollisionBehavior Constants
MAKE_SYSTEM_PROP(COLLISION_MODE_ITEM, 0);
MAKE_SYSTEM_PROP(COLLISION_MODE_BOUNDARY, 1);
MAKE_SYSTEM_PROP(COLLISION_MODE_ALL, 2);
#endif

#ifdef USE_TI_UIIOSDYNAMICITEMBEHAVIOR
-(id)createDynamicItemBehavior:(id)args
{
    return [[[TiDynamicItemBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#endif


#ifdef USE_TI_UIIOS
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(ANIMATION_CURVE_EASE_IN_OUT, UIViewAnimationOptionCurveEaseInOut, @"UI.iOS.ANIMATION_CURVE_EASE_IN_OUT", @"2.1.0", @"6.0.0", @"UI.ANIMATION_CURVE_EASE_IN_OUT");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(ANIMATION_CURVE_EASE_IN, UIViewAnimationOptionCurveEaseIn, @"UI.iOS.ANIMATION_CURVE_EASE_IN", @"2.1.0", @"6.0.0", @"UI.ANIMATION_CURVE_EASE_IN");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(ANIMATION_CURVE_EASE_OUT,UIViewAnimationOptionCurveEaseOut,  @"UI.iOS.ANIMATION_CURVE_EASE_OUT", @"2.1.0", @"6.0.0", @"UI.ANIMATION_CURVE_EASE_OUT");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(ANIMATION_CURVE_LINEAR,UIViewAnimationOptionCurveLinear, @"UI.iOS.ANIMATION_CURVE_LINEAR", @"2.1.0", @"6.0.0", @"UI.ANIMATION_CURVE_LINEAR");

MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(AUTODETECT_NONE,UIDataDetectorTypeNone, @"UI.iOS.AUTODETECT_NONE", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_NONE");
-(NSNumber*)AUTODETECT_ALL
{
    DEPRECATED_REPLACED_REMOVED(@"UI.iOS.AUTODETECT_ALL", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_ALL")
    return NUMUINTEGER(UIDataDetectorTypeAll);
}
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(AUTODETECT_PHONE,UIDataDetectorTypePhoneNumber, @"UI.iOS.AUTODETECT_PHONE", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_PHONE_NUMBERS");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(AUTODETECT_LINK,UIDataDetectorTypeLink, @"UI.iOS.AUTODETECT_LINK", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_URLS");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(AUTODETECT_ADDRESS,UIDataDetectorTypeAddress, @"UI.iOS.AUTODETECT_ADDRESS", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_MAP_ADDRESSES");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED_REMOVED(AUTODETECT_CALENDAR,UIDataDetectorTypeCalendarEvent, @"UI.iOS.AUTODETECT_CALENDAR", @"3.0.0", @"6.0.0", @"UI.AUTOLINK_CALENDAR");


MAKE_SYSTEM_STR(COLOR_GROUP_TABLEVIEW_BACKGROUND, IOS_COLOR_GROUP_TABLEVIEW_BACKGROUND);
MAKE_SYSTEM_STR(TABLEVIEW_INDEX_SEARCH, UITableViewIndexSearch);

-(NSString*)COLOR_SCROLLVIEW_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_SCROLLVIEW_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_SCROLLVIEW_TEXTURED_BACKGROUND;
}
-(NSString*)COLOR_VIEW_FLIPSIDE_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_VIEW_FLIPSIDE_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_VIEW_FLIPSIDE_BACKGROUND;
}
-(NSString*)COLOR_UNDER_PAGE_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_UNDER_PAGE_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_UNDER_PAGE_BACKGROUND;
}


MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_LINK_CLICKED,UIWebViewNavigationTypeLinkClicked);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_FORM_SUBMITTED,UIWebViewNavigationTypeFormSubmitted);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_BACK_FORWARD,UIWebViewNavigationTypeBackForward);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_RELOAD,UIWebViewNavigationTypeReload);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_FORM_RESUBMITTED,UIWebViewNavigationTypeFormResubmitted);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_OTHER,UIWebViewNavigationTypeOther);


MAKE_SYSTEM_PROP(ACTIVITY_CATEGORY_ACTION,  UIActivityCategoryAction);
MAKE_SYSTEM_PROP(ACTIVITY_CATEGORY_SHARE,   UIActivityCategoryShare);

MAKE_SYSTEM_STR(ACTIVITY_TYPE_FACEBOOK,     UIActivityTypePostToFacebook);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TWITTER,      UIActivityTypePostToTwitter);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_WEIBO,        UIActivityTypePostToWeibo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_MESSAGE,      UIActivityTypeMessage);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_MAIL,         UIActivityTypeMail);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_PRINT,        UIActivityTypePrint);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_COPY,         UIActivityTypeCopyToPasteboard);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TO_CONTACT,   UIActivityTypeAssignToContact);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_CAMERA_ROLL,  UIActivityTypeSaveToCameraRoll);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TO_READING_LIST, UIActivityTypeAddToReadingList);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_FLICKR,       UIActivityTypePostToFlickr);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_VIMEO,        UIActivityTypePostToVimeo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TENCENT_WEIBO, UIActivityTypePostToTencentWeibo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_AIRDROP,      UIActivityTypeAirDrop);


#endif

#if IS_XCODE_7
#ifdef USE_TI_UIIOSAPPLICATIONSHORTCUTS

-(id)createApplicationShortcuts:(id)args
{
    return [[[TiUIiOSApplicationShortcutsProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_COMPOSE,UIApplicationShortcutIconTypeCompose);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PLAY,UIApplicationShortcutIconTypePlay);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PAUSE,UIApplicationShortcutIconTypePause);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_ADD,UIApplicationShortcutIconTypeAdd);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_LOCATION,UIApplicationShortcutIconTypeLocation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SEARCH,UIApplicationShortcutIconTypeSearch);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SHARE,UIApplicationShortcutIconTypeShare);

#ifdef __IPHONE_9_1

MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PROHIBIT,UIApplicationShortcutIconTypeProhibit);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CONTACT,UIApplicationShortcutIconTypeContact);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_HOME,UIApplicationShortcutIconTypeHome);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MARK_LOCATION,UIApplicationShortcutIconTypeMarkLocation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_FAVORITE,UIApplicationShortcutIconTypeFavorite);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_LOVE,UIApplicationShortcutIconTypeLove);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CLOUD,UIApplicationShortcutIconTypeCloud);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_INVITATION,UIApplicationShortcutIconTypeInvitation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CONFIRMATION,UIApplicationShortcutIconTypeConfirmation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MAIL,UIApplicationShortcutIconTypeMail);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MESSAGE,UIApplicationShortcutIconTypeMessage);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_DATE,UIApplicationShortcutIconTypeDate);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TIME,UIApplicationShortcutIconTypeTime);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CAPTURE_PHOTO,UIApplicationShortcutIconTypeCapturePhoto);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CAPTURE_VIDEO,UIApplicationShortcutIconTypeCaptureVideo);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TASK,UIApplicationShortcutIconTypeTask);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TASK_COMPLETED,UIApplicationShortcutIconTypeTaskCompleted);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_ALARM,UIApplicationShortcutIconTypeAlarm);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_BOOKMARK,UIApplicationShortcutIconTypeBookmark);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SHUFFLE,UIApplicationShortcutIconTypeShuffle);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_AUDIO,UIApplicationShortcutIconTypeAudio);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_UPDATE,UIApplicationShortcutIconTypeUpdate);

#endif
#endif
#endif

//Modal Transition and Presentatiom
MAKE_SYSTEM_PROP(MODAL_TRANSITION_STYLE_COVER_VERTICAL,UIModalTransitionStyleCoverVertical);
MAKE_SYSTEM_PROP(MODAL_TRANSITION_STYLE_FLIP_HORIZONTAL,UIModalTransitionStyleFlipHorizontal);
MAKE_SYSTEM_PROP(MODAL_TRANSITION_STYLE_CROSS_DISSOLVE,UIModalTransitionStyleCrossDissolve);

MAKE_SYSTEM_PROP(MODAL_PRESENTATION_FULLSCREEN,UIModalPresentationFullScreen);
MAKE_SYSTEM_PROP(MODAL_TRANSITION_STYLE_PARTIAL_CURL,UIModalTransitionStylePartialCurl);
MAKE_SYSTEM_PROP(MODAL_PRESENTATION_PAGESHEET,UIModalPresentationPageSheet);
MAKE_SYSTEM_PROP(MODAL_PRESENTATION_FORMSHEET,UIModalPresentationFormSheet);
MAKE_SYSTEM_PROP(MODAL_PRESENTATION_CURRENT_CONTEXT,UIModalPresentationCurrentContext);
@end
#endif
