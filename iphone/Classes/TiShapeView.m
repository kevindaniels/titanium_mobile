//
//  ShapeView.m
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "TiShapeView.h"
#import "TiShapeViewProxy.h"
#import "ShapeProxy.h"

@implementation TiShapeView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
//        self.layer.opaque = NO;
//        self.layer.needsDisplayOnBoundsChange = YES;
        
    }
    return self;
}
-(void)initializeState
{
    [super initializeState];
    NSArray* shapes = [(TiShapeViewProxy*)[self proxy] shapes];
    for (int i = 0; i < [shapes count]; i++) {
        ShapeProxy* shapeProxy = [shapes objectAtIndex:i];
        [self.layer addSublayer:[shapeProxy layer]];
    }
    [(TiShapeViewProxy*)self.proxy frameSizeChanged:self.frame bounds:self.bounds];
    
}

- (void) dealloc
{
	[super dealloc];
}

- (BOOL)hasTouchableListener
{
	return YES;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [super frameSizeChanged:frame bounds:bounds];
    [(TiShapeViewProxy*)self.proxy frameSizeChanged:frame bounds:bounds];
	[self setNeedsDisplay];
}

@end