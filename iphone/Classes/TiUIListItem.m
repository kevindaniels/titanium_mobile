/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListItem.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "ImageLoader.h"
#import "Webcolor.h"

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSMutableDictionary *_initialValues;
	NSMutableDictionary *_currentValues;
	NSMutableSet *_resetKeys;
	NSDictionary *_dataItem;
	NSDictionary *_bindings;
    int _positionMask;
    BOOL _grouped;
    UIView* _bgView;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier proxy:(TiUIListItemProxy *)proxy
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = style;
		_initialValues = [[NSMutableDictionary alloc] initWithCapacity:5];
		_currentValues = [[NSMutableDictionary alloc] initWithCapacity:5];
		_resetKeys = [[NSMutableSet alloc] initWithCapacity:5];
		_proxy = [proxy retain];
		_proxy.listItem = self;
    }
    return self;
}

- (id)initWithProxy:(TiUIListItemProxy *)proxy reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = TiUIListItemTemplateStyleCustom;
		_initialValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_currentValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_resetKeys = [[NSMutableSet alloc] initWithCapacity:10];
		_proxy = [proxy retain];
		_proxy.listItem = self;
    }
    return self;
}

- (void)dealloc
{
	_proxy.listItem = nil;
	[_initialValues release];
	[_currentValues release];
	[_resetKeys release];
	[_dataItem release];
	[_proxy release];
	[_bindings release];
	[gradientLayer release];
	[backgroundGradient release];
	[selectedBackgroundGradient release];
    [_bgView removeFromSuperview];
    [_bgView release];
	[super dealloc];
}

- (NSDictionary *)bindings
{
	if (_bindings == nil) {
		NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:10];
		[[self class] buildBindingsForViewProxy:_proxy intoDictionary:dict];
		_bindings = [dict copy];
		[dict release];
	}
	return _bindings;
}

- (void)prepareForReuse
{
	RELEASE_TO_NIL(_dataItem);
	[super prepareForReuse];
}

- (void)layoutSubviews
{
    if (_bgView != nil) {
        if ([_bgView superview] == nil) {
            [self.backgroundView addSubview:_bgView];
        }
        CGRect bounds = [self.backgroundView bounds];
        if ((_positionMask == TiCellBackgroundViewPositionTop) || (_positionMask == TiCellBackgroundViewPositionSingleLine) ) {
            [_bgView setFrame:CGRectMake(0, 1, bounds.size.width, bounds.size.height -2)];
        } else {
            [_bgView setFrame:bounds];
        }
        [_bgView setNeedsDisplay];
    } else if ([self.backgroundView isKindOfClass:[TiSelectedCellBackgroundView class]]) {
        [self.backgroundView setNeedsDisplay];
    }
	[super layoutSubviews];
	if (_templateStyle == TiUIListItemTemplateStyleCustom) {
		// prevent any crashes that could be caused by unsupported layouts
		_proxy.layoutProperties->layoutStyle = TiLayoutRuleAbsolute;
		[_proxy layoutChildren:NO];
	}
}

#pragma mark - Background Support 
-(BOOL) selectedOrHighlighted
{
	return [self isSelected] || [self isHighlighted];
}

-(void) updateGradientLayer:(BOOL)useSelected withAnimation:(BOOL)animated
{
	TiGradient * currentGradient = useSelected?selectedBackgroundGradient:backgroundGradient;
    
	if(currentGradient == nil)
	{
		[gradientLayer removeFromSuperlayer];
		//Because there's the chance that the other state still has the gradient, let's keep it around.
		return;
	}
    
	CALayer * ourLayer = [self layer];
	
	if(gradientLayer == nil)
	{
		gradientLayer = [[TiGradientLayer alloc] init];
		[gradientLayer setNeedsDisplayOnBoundsChange:YES];
		[gradientLayer setFrame:[self bounds]];
	}
    
	[gradientLayer setGradient:currentGradient];
	if([gradientLayer superlayer] != ourLayer)
	{
		CALayer* contentLayer = [[self contentView] layer];
		[ourLayer insertSublayer:gradientLayer below:contentLayer];
    }
    if (animated) {
        CABasicAnimation *flash = [CABasicAnimation animationWithKeyPath:@"opacity"];
        flash.fromValue = [NSNumber numberWithFloat:0.0];
        flash.toValue = [NSNumber numberWithFloat:1.0];
        flash.duration = 1.0;
        [gradientLayer addAnimation:flash forKey:@"flashAnimation"];
    }
	[gradientLayer setNeedsDisplay];
}

-(void)setSelected:(BOOL)yn animated:(BOOL)animated
{
    [super setSelected:yn animated:animated];
    [self updateGradientLayer:yn|[self isHighlighted] withAnimation:animated];
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    [self updateGradientLayer:yn|[self isSelected] withAnimation:animated];
}

-(void) setBackgroundGradient_:(TiGradient *)newGradient
{
	if(newGradient == backgroundGradient)
	{
		return;
	}
	[backgroundGradient release];
	backgroundGradient = [newGradient retain];
	
	if(![self selectedOrHighlighted])
	{
		[self updateGradientLayer:NO withAnimation:NO];
	}
}

-(void) setSelectedBackgroundGradient_:(TiGradient *)newGradient
{
	if(newGradient == selectedBackgroundGradient)
	{
		return;
	}
	[selectedBackgroundGradient release];
	selectedBackgroundGradient = [newGradient retain];
	
	if([self selectedOrHighlighted])
	{
		[self updateGradientLayer:YES withAnimation:NO];
	}
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    _positionMask = position;
    _grouped = grouped;
}

-(void) applyBackgroundWithColor:(id)backgroundColor image:(id)backgroundImage selectedColor:(id)selectedBackgroundColor selectedImage:(id)selectedBackgroundImage
{
    UIColor* bgColor = (backgroundColor != nil) ? ([[TiUtils colorValue:backgroundColor] _color]) : nil;
    UIColor* sbgColor = (selectedBackgroundColor != nil) ? ([[TiUtils colorValue:selectedBackgroundColor] _color]) : nil;
    UIImage *bgImage = [[ImageLoader sharedLoader] loadImmediateStretchableImage:[TiUtils toURL:backgroundImage proxy:_proxy] withLeftCap:TiDimensionAuto topCap:TiDimensionAuto];
    UIImage *sbgImage = [[ImageLoader sharedLoader] loadImmediateStretchableImage:[TiUtils toURL:selectedBackgroundImage proxy:_proxy] withLeftCap:TiDimensionAuto topCap:TiDimensionAuto];
    
    if (_grouped && ![TiUtils isIOS7OrGreater]) {
        //Setting the backgroundView on grouped style is a little complicated
        //By default this is not nil. So we will add the stuff as subviews to this
        //ios7 does not seem to have a backgroundView for style grouped.
        UIView* superView = [self backgroundView];
        if (bgImage != nil) {
            if (![_bgView isKindOfClass:[UIImageView class]]) {
                [_bgView removeFromSuperview];
                RELEASE_TO_NIL(_bgView);
                _bgView = [[UIImageView alloc] initWithFrame:CGRectZero];
                _bgView.autoresizingMask = UIViewAutoresizingFlexibleWidth;
                [superView addSubview:_bgView];
            }
            [(UIImageView*)_bgView setImage:bgImage];
            [_bgView setBackgroundColor:((bgColor == nil) ? [UIColor clearColor] : bgColor)];
        } else {
            if (bgColor != nil) {
                if (![_bgView isKindOfClass:[TiSelectedCellBackgroundView class]]) {
                    [_bgView removeFromSuperview];
                    RELEASE_TO_NIL(_bgView);
                    _bgView = [[TiSelectedCellBackgroundView alloc] initWithFrame:CGRectZero];
                    _bgView.autoresizingMask = UIViewAutoresizingFlexibleWidth;
                    [superView addSubview:_bgView];
                }
                ((TiSelectedCellBackgroundView*)_bgView).grouped = _grouped;
                ((TiSelectedCellBackgroundView*)_bgView).fillColor = bgColor;
                ((TiSelectedCellBackgroundView*)_bgView).position = _positionMask;
                
            } else {
                [_bgView removeFromSuperview];
                RELEASE_TO_NIL(_bgView);
            }
        }
    } else {
        if (bgImage != nil) {
            //Set the backgroundView to ImageView and set its backgroundColor to bgColor
            if ([self.backgroundView isKindOfClass:[UIImageView class]]) {
                [(UIImageView*)self.backgroundView setImage:bgImage];
                [(UIImageView*)self.backgroundView setBackgroundColor:((bgColor == nil) ? [UIColor clearColor] : bgColor)];
            } else {
                UIImageView *view_ = [[[UIImageView alloc] initWithFrame:CGRectZero] autorelease];
                [view_ setImage:bgImage];
                [view_ setBackgroundColor:((bgColor == nil) ? [UIColor clearColor] : bgColor)];
                self.backgroundView = view_;
            }
        } else {
            if (bgColor != nil) {
                if (![self.backgroundView isKindOfClass:[TiSelectedCellBackgroundView class]]) {
                    self.backgroundView = [[[TiSelectedCellBackgroundView alloc] initWithFrame:CGRectZero] autorelease];
                }
                TiSelectedCellBackgroundView *bgView = (TiSelectedCellBackgroundView*)self.backgroundView;
                bgView.grouped = _grouped;
                bgView.fillColor = bgColor;
                [bgView setPosition:_positionMask];
            } else {
                self.backgroundView = nil;
            }
        }
    }
    
    if (sbgImage != nil) {
        if ([self.selectedBackgroundView isKindOfClass:[UIImageView class]]) {
            [(UIImageView*)self.selectedBackgroundView setImage:sbgImage];
            [(UIImageView*)self.selectedBackgroundView setBackgroundColor:((sbgColor == nil) ? [UIColor clearColor] : sbgColor)];
        } else {
            UIImageView *view_ = [[[UIImageView alloc] initWithFrame:CGRectZero] autorelease];
            [view_ setImage:sbgImage];
            [view_ setBackgroundColor:((sbgColor == nil) ? [UIColor clearColor] : sbgColor)];
            self.selectedBackgroundView = view_;
        }
    } else {
        if (![self.selectedBackgroundView isKindOfClass:[TiSelectedCellBackgroundView class]]) {
            self.selectedBackgroundView = [[[TiSelectedCellBackgroundView alloc] initWithFrame:CGRectZero] autorelease];
        }
        TiSelectedCellBackgroundView *selectedBGView = (TiSelectedCellBackgroundView*)self.selectedBackgroundView;
        selectedBGView.grouped = _grouped;
        if (sbgColor == nil) {
            switch (self.selectionStyle) {
                case UITableViewCellSelectionStyleGray:sbgColor = [Webcolor webColorNamed:@"#bbb"];break;
                case UITableViewCellSelectionStyleNone:sbgColor = [UIColor clearColor];break;
                case UITableViewCellSelectionStyleBlue:sbgColor = [Webcolor webColorNamed:@"#0272ed"];break;
                default:sbgColor = [TiUtils isIOS7OrGreater] ? [Webcolor webColorNamed:@"#e0e0e0"] : [Webcolor webColorNamed:@"#0272ed"];break;
            }
        }
        selectedBGView.fillColor = sbgColor;
        [selectedBGView setPosition:_positionMask];
    }
}

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
{
	id template = [_dataItem objectForKey:@"template"];
	id otherTemplate = [otherItem objectForKey:@"template"];
	BOOL same = (template == otherTemplate) || [template isEqual:otherTemplate];
	if (same) {
		id propertiesValue = [_dataItem objectForKey:@"properties"];
		NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
		id heightValue = [properties objectForKey:@"height"];
		
		propertiesValue = [otherItem objectForKey:@"properties"];
		properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
		id otherHeightValue = [properties objectForKey:@"height"];
		same = (heightValue == otherHeightValue) || [heightValue isEqual:otherHeightValue];
	}
	return same;
}

- (void)setDataItem:(NSDictionary *)dataItem
{
	_dataItem = [dataItem retain];
	[_resetKeys addObjectsFromArray:[_currentValues allKeys]];
	id propertiesValue = [dataItem objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
	switch (_templateStyle) {
		case UITableViewCellStyleSubtitle:
		case UITableViewCellStyleValue1:
		case UITableViewCellStyleValue2:
			self.detailTextLabel.text = [[properties objectForKey:@"subtitle"] description];
            self.detailTextLabel.backgroundColor = [UIColor clearColor];
			// pass through
		case UITableViewCellStyleDefault:
			self.textLabel.text = [[properties objectForKey:@"title"] description];
            self.textLabel.backgroundColor = [UIColor clearColor];
			if (_templateStyle != UITableViewCellStyleValue2) {
				id imageValue = [properties objectForKey:@"image"];
				if ([self shouldUpdateValue:imageValue forKeyPath:@"imageView.image"]) {
					NSURL *imageUrl = [TiUtils toURL:imageValue proxy:_proxy];
					UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:imageUrl];
					if (image != nil) {
						[self recordChangeValue:imageValue forKeyPath:@"imageView.image" withBlock:^{
							self.imageView.image = image;
						}];
					}
				}
			}

			id fontValue = [properties objectForKey:@"font"];
			if ([self shouldUpdateValue:fontValue forKeyPath:@"textLabel.font"]) {
				UIFont *font = (fontValue != nil) ? [[TiUtils fontValue:fontValue] font] : nil;
				if (font != nil) {
					[self recordChangeValue:fontValue forKeyPath:@"textLabel.font" withBlock:^{
						[self.textLabel setFont:font];
					}];
				}
			}

			id colorValue = [properties objectForKey:@"color"];
			if ([self shouldUpdateValue:colorValue forKeyPath:@"textLabel.color"]) {
				UIColor *color = colorValue != nil ? [[TiUtils colorValue:colorValue] _color] : nil;
				if (color != nil) {
					[self recordChangeValue:colorValue forKeyPath:@"textLabel.color" withBlock:^{
						[self.textLabel setTextColor:color];
					}];
				}
			}
			break;
			
		default:
			[dataItem enumerateKeysAndObjectsUsingBlock:^(NSString *bindId, id dict, BOOL *stop) {
				if (![dict isKindOfClass:[NSDictionary class]] || [bindId isEqualToString:@"properties"]) {
					return;
				}
				id bindObject = [self valueForUndefinedKey:bindId];
				if (bindObject != nil) {
					BOOL reproxying = NO;
					if ([bindObject isKindOfClass:[TiProxy class]]) {
						[bindObject setReproxying:YES];
						reproxying = YES;
					}
					[(NSDictionary *)dict enumerateKeysAndObjectsUsingBlock:^(NSString *key, id value, BOOL *stop) {
						NSString *keyPath = [NSString stringWithFormat:@"%@.%@", bindId, key];
						if ([self shouldUpdateValue:value forKeyPath:keyPath]) {
							[self recordChangeValue:value forKeyPath:keyPath withBlock:^{
								[bindObject setValue:value forKey:key];
							}];
						}
					}];
					if (reproxying) {
						[bindObject setReproxying:NO];
					}
				}
			}];
			break;
	}
	id accessoryTypeValue = [properties objectForKey:@"accessoryType"];
	if ([self shouldUpdateValue:accessoryTypeValue forKeyPath:@"accessoryType"]) {
		if ([accessoryTypeValue isKindOfClass:[NSNumber class]]) {
			UITableViewCellAccessoryType accessoryType = [accessoryTypeValue unsignedIntegerValue];
			[self recordChangeValue:accessoryTypeValue forKeyPath:@"accessoryType" withBlock:^{
				self.accessoryType = accessoryType;
			}];
		}
	}
	id selectionStyleValue = [properties objectForKey:@"selectionStyle"];
	if ([self shouldUpdateValue:selectionStyleValue forKeyPath:@"selectionStyle"]) {
		if ([selectionStyleValue isKindOfClass:[NSNumber class]]) {
			UITableViewCellSelectionStyle selectionStyle = [selectionStyleValue unsignedIntegerValue];
			[self recordChangeValue:selectionStyleValue forKeyPath:@"selectionStyle" withBlock:^{
				self.selectionStyle = selectionStyle;
			}];
		}
	}
    
    id backgroundGradientValue = [properties objectForKey:@"backgroundGradient"];
    [self setBackgroundGradient_:backgroundGradientValue];
	
    
    id selectedBackgroundGradientValue = [properties objectForKey:@"selectedBackgroundGradient"];
    [self setSelectedBackgroundGradient_:selectedBackgroundGradientValue];
	

	id backgroundColorValue = [properties objectForKey:@"backgroundColor"];
	id selectedbackgroundColorValue = [properties objectForKey:@"selectedBackgroundColor"];
	id backgroundImageValue = [properties objectForKey:@"backgroundImage"];
	id selectedBackgroundImageValue = [properties objectForKey:@"selectedBackgroundImage"];
	[self applyBackgroundWithColor:backgroundColorValue image:backgroundImageValue selectedColor:selectedbackgroundColorValue selectedImage:selectedBackgroundImageValue];
	[_resetKeys enumerateObjectsUsingBlock:^(NSString *keyPath, BOOL *stop) {
		id value = [_initialValues objectForKey:keyPath];
		[self setValue:(value != [NSNull null] ? value : nil) forKeyPath:keyPath];
		[_currentValues removeObjectForKey:keyPath];
	}];
	[_resetKeys removeAllObjects];
}

- (id)valueForUndefinedKey:(NSString *)key
{
	return [self.bindings objectForKey:key];
}

- (void)recordChangeValue:(id)value forKeyPath:(NSString *)keyPath withBlock:(void(^)(void))block
{
	if ([_initialValues objectForKey:keyPath] == nil) {
		id initialValue = [self valueForKeyPath:keyPath];
		[_initialValues setObject:(initialValue != nil ? initialValue : [NSNull null]) forKey:keyPath];
	}
	block();
	if (value != nil) {
		[_currentValues setObject:value forKey:keyPath];
	} else {
		[_currentValues removeObjectForKey:keyPath];
	}
	[_resetKeys removeObject:keyPath];
}

- (BOOL)shouldUpdateValue:(id)value forKeyPath:(NSString *)keyPath
{
	id current = [_currentValues objectForKey:keyPath];
	BOOL sameValue = ((current == value) || [current isEqual:value]);
	if (sameValue) {
		[_resetKeys removeObject:keyPath];
	}
	return !sameValue;
}

#pragma mark - Static 

+ (void)buildBindingsForViewProxy:(TiViewProxy *)viewProxy intoDictionary:(NSMutableDictionary *)dict
{
	[viewProxy.children enumerateObjectsUsingBlock:^(TiViewProxy *childViewProxy, NSUInteger idx, BOOL *stop) {
		[[self class] buildBindingsForViewProxy:childViewProxy intoDictionary:dict];
	}];
	id bindId = [viewProxy valueForKey:@"bindId"];
	if (bindId != nil) {
		[dict setObject:viewProxy forKey:bindId];
	}
}

@end

#endif
