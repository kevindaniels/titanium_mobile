/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionWrapperViewProxy.h"
#import "TiUtils.h"
#import "TiUICollectionWrapperView.h"
#import "TiUICollectionViewProxy.h"
#import "TiUICollectionView.h"


static void SetEventOverrideDelegateRecursive(NSArray *children, id<TiViewEventOverrideDelegate> eventOverrideDelegate);


@interface TiUICollectionView()
-(UITableViewCell *) forceCellForRowAtIndexPath:(NSIndexPath *)indexPath;
@end

@implementation TiUICollectionWrapperViewProxy {
    TiUICollectionViewProxy *_listViewProxy; // weak
    NSDictionary *_bindings;
    NSDictionary *_templateProperties;
    NSMutableDictionary *_initialValues;
    NSMutableDictionary *_currentValues;
    NSMutableSet *_resetKeys;
    BOOL unarchived;
    BOOL enumeratingResetKeys;
    BOOL _buildingBindings;
}

@synthesize wrapperView = _wrapperView;
@synthesize indexPath = _indexPath;

- (id)initWithCollectionViewProxy:(TiUICollectionViewProxy *)listViewProxy inContext:(id<TiEvaluator>)context
{
    self = [self _initWithPageContext:context];
    if (self) {
        unarchived = NO;
        enumeratingResetKeys = NO;
        _buildingBindings = NO;
        _initialValues = [[NSMutableDictionary alloc] initWithCapacity:10];
        _currentValues = [[NSMutableDictionary alloc] initWithCapacity:10];
        _resetKeys = [[NSMutableSet alloc] initWithCapacity:10];
        _listViewProxy = listViewProxy;
        eventOverrideDelegate = self; // to make sure we also override events
        [context.krollContext invokeBlockOnThread:^{
            [context registerProxy:self];
            //Reusable cell will keep native proxy alive.
            //This proxy will keep its JS object alive.
            [self rememberSelf];
        }];
    }
    return self;
}

+(BOOL)shouldRegisterOnInit
{
    //Since this is initialized on main thread,
    //there is no need to register on init. Registration
    //done later on JS thread (See above)
    return NO;
}

-(void)deregisterProxy:(id<TiEvaluator>)context
{
    //Aggressive removal of children on deallocation of cell
    [self removeAllChildren:nil];
    [self windowDidClose];
    //Go ahead and unprotect JS object and mark context closed
    //(Since cell no longer exists, the proxy is inaccessible)
    [context.krollContext invokeBlockOnThread:^{
        [self forgetSelf];
        [self contextShutdown:context];
    }];
}

-(NSString*)apiName
{
    return @"Ti.UI.CollectionItem";
}

- (id)init
{
    self = [super init];
    if (self) {
    }
    return self;
}

-(void)cleanup
{
    //    if (_listViewProxy) {
    //        [_listViewProxy forgetProxy:self];
    //        [self.pageContext unregisterProxy:self];
    //        _listViewProxy = nil;
    //    }
    _wrapperView = nil;
}

-(void) setWrapperView:(TiUICollectionWrapperView *)wrapperView
{
    //we must not retain the item or we get a cyclic retain problem
    //    RELEASE_TO_NIL(_listItem);
    _wrapperView = wrapperView;
    if (wrapperView)
    {
        view = wrapperView.viewHolder;
        [view initializeState];
        viewInitialized = YES;
        parentVisible = YES;
        readyToCreateView = YES;
        windowOpened = YES;
    }
    else {
        view = nil;
        viewInitialized = NO;
        parentVisible = NO;
        readyToCreateView = NO;
        windowOpened = NO;
    }
}

-(void)dealloc
{
    [self cleanup];
    RELEASE_TO_NIL(_initialValues)
    RELEASE_TO_NIL(_currentValues)
    RELEASE_TO_NIL(_resetKeys)
    RELEASE_TO_NIL(_indexPath)
    RELEASE_TO_NIL(_bindings)
    RELEASE_TO_NIL(_templateProperties)
    [super dealloc];
}

-(TiProxy*)parentForBubbling
{
    return _listViewProxy;
}


- (void)detachView
{
    view = nil;
    [super detachView];
}

-(BOOL)viewAttached
{
    return _wrapperView != nil;
}

- (void)unarchiveFromTemplate:(id)viewTemplate withEvents:(BOOL)withEvents
{
    [super unarchiveFromTemplate:viewTemplate withEvents:withEvents];
    //lets store the default template props
    _templateProperties = [[NSDictionary dictionaryWithDictionary:[self allProperties]] retain];
    if (withEvents) SetEventOverrideDelegateRecursive(self.children, self);
    unarchived = YES;
    [self.bindings enumerateKeysAndObjectsUsingBlock:^(id binding, id bindObject, BOOL *stop) {
        [[bindObject allProperties] enumerateKeysAndObjectsUsingBlock:^(id key, id prop, BOOL *stop) {
            [_initialValues setValue:prop forKey:[NSString stringWithFormat:@"%@.%@",binding, key]];
        }];
    }];
    [_initialValues addEntriesFromDictionary:[self allProperties]];
    
}

- (NSDictionary *)bindings
{
    if (_bindings == nil &&  unarchived && !_buildingBindings) {
        _buildingBindings = YES;
        NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:10];
        [self buildBindingsForViewProxy:self intoDictionary:dict];
        _bindings = [dict copy];
        [dict release];
        _buildingBindings = NO;
    }
    return _bindings;
}

-(void)setValue:(id)value forKey:(NSString *)key
{
    if ([self shouldUpdateValue:value forKeyPath:key]) {
        [self recordChangeValue:value forKeyPath:key withBlock:^{
            [super setValue:value forKey:key];
        }];
    }
}

-(void)setValue:(id)value forKeyPath:(NSString *)keyPath
{
    if([keyPath isEqualToString:@"properties"])
    {
        [self setValuesForKeysWithDictionary:value];
    }
    else if ([value isKindOfClass:[NSDictionary class]]) {
        id bindObject = [self.bindings objectForKey:keyPath];
        if (bindObject != nil) {
            NSArray * keySequence = [bindObject keySequence];
            for (NSString * key in keySequence)
            {
                if ([value objectForKey:key]) {
                    id value2 = [value objectForKey:key];
                    NSString *newKeyPath = [NSString stringWithFormat:@"%@.%@", keyPath, key];
                    if ([self shouldUpdateValue:value2 forKeyPath:newKeyPath]) {
                        [self recordChangeValue:value2 forKeyPath:newKeyPath withBlock:^{
                            [bindObject setValue:value2 forKey:key];
                        }];
                    }
                }
            }
            [(NSDictionary *)value enumerateKeysAndObjectsUsingBlock:^(NSString *key, id value2, BOOL *stop) {
                if (![keySequence containsObject:key])
                {
                    NSString *newKeyPath = [NSString stringWithFormat:@"%@.%@", keyPath, key];
                    if ([self shouldUpdateValue:value2 forKeyPath:newKeyPath]) {
                        [self recordChangeValue:value2 forKeyPath:newKeyPath withBlock:^{
                            id obj = [bindObject valueForUndefinedKey:key];
                            if ([obj isKindOfClass:[TiProxy class]] && [value2 isKindOfClass:[NSDictionary class]]) {
                                [obj setValuesForKeysWithDictionary:value2];
                            }
                            else {
                                [bindObject setValue:value2 forKey:key];
                            }
                        }];
                    }
                }
                
            }];
        }
        else {
            [super setValue:value forKeyPath:keyPath];
        }
    }
    else [super setValue:value forKeyPath:keyPath];
}



-(void)configurationStart:(BOOL)recursive
{
    [_wrapperView configurationStart];
    [super configurationStart:recursive];
}

-(void)configurationSet:(BOOL)recursive
{
    [super configurationSet:recursive];
    [_wrapperView configurationSet];
}

- (void)setDataItem:(NSDictionary *)dataItem
{
    [self configurationStart:YES];
    [_resetKeys addObjectsFromArray:[_currentValues allKeys]];
    
    NSMutableDictionary* listProps = [NSMutableDictionary dictionaryWithDictionary:[_listViewProxy propertiesForItems]];
    if (_templateProperties) {
        [listProps removeObjectsForKeys:[_templateProperties allKeys]];
    }
    if ([dataItem objectForKey:@"properties"])
    {
        [listProps removeObjectsForKeys:[[dataItem objectForKey:@"properties"] allKeys]];
    }
    
    [self.bindings enumerateKeysAndObjectsUsingBlock:^(id key, id bindObject, BOOL *stop) {
        if ([bindObject isKindOfClass:[TiProxy class]]) {
            [bindObject setReproxying:YES];
        }
    }];
    
    [self setValuesForKeysWithDictionary:listProps];
    
    [dataItem enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [self setValue:obj forKeyPath:key];
    }];
    
    enumeratingResetKeys = YES;
    [_resetKeys enumerateObjectsUsingBlock:^(NSString *keyPath, BOOL *stop) {
        id value = [_initialValues objectForKey:keyPath];
        [super setValue:(value != [NSNull null] ? value : nil) forKeyPath:keyPath];
        [_currentValues removeObjectForKey:keyPath];
    }];
    [_resetKeys removeAllObjects];
    enumeratingResetKeys = NO;
    
    [self.bindings enumerateKeysAndObjectsUsingBlock:^(id key, id bindObject, BOOL *stop) {
        if ([bindObject isKindOfClass:[TiProxy class]]) {
            [bindObject setReproxying:NO];
        }
    }];
    
    [self configurationSet:YES];
}

- (id)valueForUndefinedKey:(NSString *)key
{
    if (!_buildingBindings && [self.bindings objectForKey:key])
        return [self.bindings objectForKey:key];
    return [super valueForUndefinedKey:key];
}


- (void)recordChangeValue:(id)value forKeyPath:(NSString *)keyPath withBlock:(void(^)(void))block
{
    //	if ([_initialValues objectForKey:keyPath] == nil) {
    //		id initialValue = [self valueForKeyPath:keyPath];
    //		[_initialValues setObject:(initialValue != nil ? initialValue : [NSNull null]) forKey:keyPath];
    //	}
    block();
    if (!unarchived) {
        return;
    }
    if (value != nil) {
        [_currentValues setObject:value forKey:keyPath];
    } else {
        [_currentValues removeObjectForKey:keyPath];
    }
    if (!enumeratingResetKeys) [_resetKeys removeObject:keyPath];
}

- (BOOL)shouldUpdateValue:(id)value forKeyPath:(NSString *)keyPath
{
    id current = [_currentValues objectForKey:keyPath];
    BOOL sameValue = ((current == value) || [current isEqual:value]);
    if (sameValue && !enumeratingResetKeys) {
        [_resetKeys removeObject:keyPath];
    }
    return !sameValue;
}

#pragma mark - Static

- (void)buildBindingsForViewProxy:(TiProxy *)viewProxy intoDictionary:(NSMutableDictionary *)dict
{
    if ([viewProxy isKindOfClass:[TiUICollectionWrapperViewProxy class]]) { //toplevel
        TiUICollectionWrapperView* view = ((TiUICollectionWrapperViewProxy*)viewProxy).wrapperView;
    }

    if ([viewProxy isKindOfClass:[TiParentingProxy class]]) {
        NSArray* myChildren = [(TiParentingProxy*)viewProxy children];
        [myChildren enumerateObjectsUsingBlock:^(TiProxy *childViewProxy, NSUInteger idx, BOOL *stop) {
            [self buildBindingsForViewProxy:childViewProxy intoDictionary:dict];
        }];
        
    }
    
    if (![viewProxy isKindOfClass:[TiUICollectionWrapperViewProxy class]]) {
        id bindId = [viewProxy valueForKey:@"bindId"];
        if (bindId != nil) {
            [dict setObject:viewProxy forKey:bindId];
        }
    }
}

-(BOOL)canHaveControllerParent
{
    return NO;
}

#pragma mark - TiViewEventOverrideDelegate

- (NSDictionary *)overrideEventObject:(NSDictionary *)eventObject forEvent:(NSString *)eventType fromViewProxy:(TiViewProxy *)viewProxy
{
    NSMutableDictionary *updatedEventObject = [eventObject mutableCopy];
    [updatedEventObject setObject:NUMINT(_indexPath.section) forKey:@"sectionIndex"];
    [updatedEventObject setObject:NUMINT(_indexPath.row) forKey:@"itemIndex"];
    [updatedEventObject setObject:[_listViewProxy sectionForIndex:_indexPath.section] forKey:@"section"];
    id propertiesValue = [_wrapperView.dataItem objectForKey:@"properties"];
    NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
    id itemId = [properties objectForKey:@"itemId"];
    if (itemId != nil) {
        [updatedEventObject setObject:itemId forKey:@"itemId"];
    }
    id bindId = [viewProxy valueForKey:@"bindId"];
    if (bindId != nil) {
        [updatedEventObject setObject:bindId forKey:@"bindId"];
    }
//    [_listViewProxy didOverrideEvent:eventType forItem:self];
    return [updatedEventObject autorelease];
}

- (void)viewProxy:(TiProxy *)viewProxy updatedValue:(id)value forType:(NSString *)type;
{
    [self.bindings enumerateKeysAndObjectsUsingBlock:^(id binding, id bindObject, BOOL *stop) {
        if (bindObject == viewProxy) {
            [[_wrapperView.dataItem objectForKey:binding] setValue:value forKey:type];
            [_currentValues setValue:value forKey:[NSString stringWithFormat:@"%@.%@", binding, type]];
            return;
        }
    }];
}


-(TiViewAnimationStep*)runningAnimation
{
    return [_listViewProxy runningAnimation];
}

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoFill;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

@end

static void SetEventOverrideDelegateRecursive(NSArray *children, id<TiViewEventOverrideDelegate> eventOverrideDelegate)
{
    [children enumerateObjectsUsingBlock:^(TiProxy *child, NSUInteger idx, BOOL *stop) {
        child.eventOverrideDelegate = eventOverrideDelegate;
        if ([child isKindOfClass:[TiParentingProxy class]]) {
            SetEventOverrideDelegateRecursive(((TiParentingProxy*)child).children, eventOverrideDelegate);
        }
    }];
}

#endif
