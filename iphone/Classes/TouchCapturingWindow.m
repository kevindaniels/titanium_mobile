//
//  TouchCapturingWindow.m
//  Titanium
//
//  Created by Martin Guillon on 16/02/14.
//
//

#import "TouchCapturingWindow.h"
#import "TiUIView.h"

@implementation TouchCapturingWindow


- (void)sendEvent:(UIEvent *)event {
    UITouch *touch = [[event allTouches] anyObject];
    
    UIView* view = [touch view];
    //on ios7 there are at least 11 levels when showing videos controls and touching them! :s
    for (int i=0; i<11; i++) {
//        NSString * proxyName = NSStringFromClass([view class]);
        if (IS_OF_CLASS(view, TiUIView))
        {
//            TiUIView* tiview = ([[view superview] isKindOfClass:[TiUIView class]])?(TiUIView*)[view superview]:nil;
            [(TiUIView*)view onInterceptTouchEvent:event];
//            if (tiview && [tiview interactionEnabled]) {
//                if (touch.phase == UITouchPhaseBegan) {
//                    [tiview processTouchesBegan:[event allTouches] withEvent:event];
//                }
//                else if (touch.phase == UITouchPhaseMoved) {
//                    [tiview processTouchesMoved:[event allTouches] withEvent:event];
//
//                }
//                else if (touch.phase == UITouchPhaseEnded) {
//                    [tiview processTouchesEnded:[event allTouches] withEvent:event];
//
//                }
//                else if (touch.phase == UITouchPhaseCancelled) {
//                    [tiview processTouchesCancelled:[event allTouches] withEvent:event];
//
//                }
//                
//            }
            break;
        }
        view = [view superview];
    }
    [super sendEvent:event];
}


@end
