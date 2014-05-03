/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListViewProxy.h"
#import "TiUIListView.h"
#import "TiUIListItem.h"
#import "TiUtils.h"
#import "TiViewTemplate.h"
#import "TiTableView.h"

@interface TiUIListViewProxy ()
@property (nonatomic, readwrite) TiUIListView *listView;
@end

@implementation TiUIListViewProxy {
	NSMutableArray *_sections;
	NSMutableArray *_operationQueue;
	pthread_mutex_t _operationQueueMutex;
	pthread_rwlock_t _markerLock;
	NSIndexPath *marker;
    NSDictionary* _propertiesForItems;
}
@synthesize propertiesForItems = _propertiesForItems;

static NSArray* keysToGetFromListView;
-(NSArray *)keysToGetFromListView
{
	if (keysToGetFromListView == nil)
	{
		keysToGetFromListView = [[NSArray arrayWithObjects:@"tintColor",@"accessoryType",@"selectionStyle",@"selectedBackgroundColor",@"selectedBackgroundImage",@"selectedBackgroundGradient", @"unHighlightOnSelect", nil] retain];
	}
	return keysToGetFromListView;
}

static NSDictionary* listViewKeysToReplace;
-(NSDictionary *)listViewKeysToReplace
{
	if (listViewKeysToReplace == nil)
	{
		listViewKeysToReplace = [@{@"selectedBackgroundColor": @"backgroundSelectedColor",
                                   @"selectedBackgroundGradient": @"backgroundSelectedGradient",
                                   @"selectedBackgroundImage": @"backgroundSelectedImage"
                                   } retain];
	}
	return listViewKeysToReplace;
}

- (id)init
{
    self = [super init];
    if (self) {
		_sections = [[NSMutableArray alloc] initWithCapacity:4];
		_operationQueue = [[NSMutableArray alloc] initWithCapacity:10];
		pthread_mutex_init(&_operationQueueMutex,NULL);
		pthread_rwlock_init(&_markerLock,NULL);
    }
    return self;
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    [self initializeProperty:@"canScroll" defaultValue:NUMBOOL(YES)];
    [self initializeProperty:@"caseInsensitiveSearch" defaultValue:NUMBOOL(YES)];
    [super _initWithProperties:properties];
}

-(NSString*)apiName
{
    return @"Ti.UI.ListView";
}

- (void)dealloc
{
	[_operationQueue release];
	pthread_mutex_destroy(&_operationQueueMutex);
	pthread_rwlock_destroy(&_markerLock);
    RELEASE_TO_NIL(_sections);
	RELEASE_TO_NIL(marker);
    RELEASE_TO_NIL(_propertiesForItems);
    [super dealloc];
}

- (TiUIListView *)listView
{
	return (TiUIListView *)self.view;
}

-(void)setValue:(id)value forKey:(NSString *)key
{
    if ([[self keysToGetFromListView] containsObject:key])
    {
        if (_propertiesForItems == nil)
        {
            _propertiesForItems = [[NSMutableDictionary alloc] init];
        }
        if ([[self listViewKeysToReplace] valueForKey:key]) {
            [_propertiesForItems setValue:value forKey:[[self listViewKeysToReplace] valueForKey:key]];
        }
        else {
            [_propertiesForItems setValue:value forKey:key];
        }
    }
    [super setValue:value forKey:key];
}

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block
{
    [self dispatchUpdateAction:block animated:YES];
}
-(void)dispatchUpdateAction:(void(^)(UITableView *tableView))block animated:(BOOL)animated
{
	if (view == nil) {
		block(nil);
		return;
	}
    
    if ([self.listView isSearchActive]) {
        block(nil);
        TiThreadPerformOnMainThread(^{
            [self.listView updateSearchResults:nil];
        }, NO);
        return;
    }
    
	BOOL triggerMainThread;
	pthread_mutex_lock(&_operationQueueMutex);
	triggerMainThread = [_operationQueue count] == 0;
	[_operationQueue addObject:Block_copy(block)];
    pthread_mutex_unlock(&_operationQueueMutex);
	if (triggerMainThread) {
		TiThreadPerformOnMainThread(^{
            if (animated)
            {
			[self processUpdateActions];
            }
            else {
                [UIView setAnimationsEnabled:NO];
                [self processUpdateActions];
                [UIView setAnimationsEnabled:YES];
            }
		}, NO);
	}
}

- (void)dispatchBlock:(void(^)(UITableView *tableView))block
{
	if (view == nil) {
		block(nil);
		return;
	}
	if ([NSThread isMainThread]) {
		return block(self.listView.tableView);
	}
	TiThreadPerformOnMainThread(^{
		block(self.listView.tableView);
	}, YES);
}

- (id)dispatchBlockWithResult:(id(^)(void))block
{
	if ([NSThread isMainThread]) {
		return block();
	}
	
	__block id result = nil;
	TiThreadPerformOnMainThread(^{
		result = [block() retain];
	}, YES);
	return [result autorelease];
}

- (void)processUpdateActions
{
	UITableView *tableView = self.listView.tableView;
	BOOL removeHead = NO;
    CGPoint offset;
	while (YES) {
		void (^block)(UITableView *) = nil;
		pthread_mutex_lock(&_operationQueueMutex);
		if (removeHead) {
			[_operationQueue removeObjectAtIndex:0];
		}
		if ([_operationQueue count] > 0) {
			block = [_operationQueue objectAtIndex:0];
			removeHead = YES;
		}
		pthread_mutex_unlock(&_operationQueueMutex);
		if (block != nil) {
            offset = [tableView contentOffset];
			[tableView beginUpdates];
			block(tableView);
			[tableView endUpdates];
            [tableView setContentOffset:offset animated:NO];
			Block_release(block);
		} else {
			[self.listView updateIndicesForVisibleRows];
			return;
		}
	}
}

- (TiUIListSectionProxy *)sectionForIndex:(NSUInteger)index
{
	if (index < [_sections count]) {
		return [_sections objectAtIndex:index];
	}
	return nil;
}

- (void) deleteSectionAtIndex:(NSUInteger)index
{
    if ([_sections count] <= index) {
        DebugLog(@"[WARN] ListViewProxy: Delete section index is out of range");
        return;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:index];
    [_sections removeObjectAtIndex:index];
    section.delegate = nil;
    [_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
        section.sectionIndex = idx;
    }];
    [self forgetProxy:section];
}

- (NSArray *)keySequence
{
	static dispatch_once_t onceToken;
	static NSArray *keySequence = nil;
	dispatch_once(&onceToken, ^{
		keySequence = [[NSArray alloc] initWithObjects:@"style", @"templates", @"defaultItemTemplate", @"sections", @"backgroundColor",nil];
	});
	return keySequence;
}

- (void)viewDidInitialize
{
	[self.listView tableView];
    [super viewDidInitialize];
}

- (void)willShow
{
	[self.listView deselectAll:YES];
	[super willShow];
}

#pragma mark - Public API

- (NSArray *)sections
{
	return [self dispatchBlockWithResult:^() {
		return [[_sections copy] autorelease];
	}];
}

- (NSNumber *)sectionCount
{
	return [self dispatchBlockWithResult:^() {
		return [NSNumber numberWithUnsignedInteger:[_sections count]];
	}];
}

- (void)setSections:(id)args
{
	ENSURE_TYPE_OR_NIL(args,NSArray);
	NSMutableArray *insertedSections = [args mutableCopy];
    for (int i = 0; i < [insertedSections count]; i++) {
        id section = [insertedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUIListSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
            [insertedSections replaceObjectAtIndex:i withObject:section];
        }
        else {
		ENSURE_TYPE(section, TiUIListSectionProxy);
        }
		[self rememberProxy:section];
    }
	[self dispatchBlock:^(UITableView *tableView) {
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = nil;
			if (![insertedSections containsObject:section]) {
				[self forgetProxy:section];
			}
		}];
		[_sections release];
		_sections = [insertedSections retain];
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = self;
			section.sectionIndex = idx;
		}];
		[tableView reloadData];
	}];
	[insertedSections release];
}

- (void)appendSection:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	id arg = [args objectAtIndex:0];
	NSArray *appendedSections = [arg isKindOfClass:[NSArray class]] ? arg : [NSArray arrayWithObject:arg];
	if ([appendedSections count] == 0) {
		return;
	}
	NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    NSMutableArray *insertedSections = [NSMutableArray arrayWithCapacity:[appendedSections count]];
    for (int i = 0; i < [appendedSections count]; i++) {
        id section = [appendedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUIListSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
        }
        else {
		ENSURE_TYPE(section, TiUIListSectionProxy);
        }
		[self rememberProxy:section];
        [insertedSections addObject:section];
    }
	[self dispatchUpdateAction:^(UITableView *tableView) {
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		[insertedSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				NSUInteger insertIndex = [_sections count];
				[_sections addObject:section];
				section.delegate = self;
				section.sectionIndex = insertIndex;
				[indexSet addIndex:insertIndex];
			} else {
				DebugLog(@"[WARN] ListView: Attempt to append exising section");
			}
		}];
		if ([indexSet count] > 0) {
			[tableView insertSections:indexSet withRowAnimation:animation];
		}
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)deleteSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger deleteIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections count] <= deleteIndex) {
			DebugLog(@"[WARN] ListView: Delete section index is out of range");
			return;
		}
		TiUIListSectionProxy *section = [_sections objectAtIndex:deleteIndex];
		[_sections removeObjectAtIndex:deleteIndex];
		section.delegate = nil;
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView deleteSections:[NSIndexSet indexSetWithIndex:deleteIndex] withRowAnimation:animation];
		[self forgetProxy:section];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)insertSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger insertIndex = [TiUtils intValue:[args objectAtIndex:0]];
	id arg = [args objectAtIndex:1];
	NSArray *insertSections = [arg isKindOfClass:[NSArray class]] ? arg : [NSArray arrayWithObject:arg];
	if ([insertSections count] == 0) {
		return;
	}
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
		ENSURE_TYPE(section, TiUIListSectionProxy);
		[self rememberProxy:section];
	}];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections count] < insertIndex) {
			DebugLog(@"[WARN] ListView: Insert section index is out of range");
			[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
				[self forgetProxy:section];
			}];
			return;
		}
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		__block NSUInteger index = insertIndex;
		[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				[_sections insertObject:section atIndex:index];
				section.delegate = self;
				[indexSet addIndex:index];
				++index;
			} else {
				DebugLog(@"[WARN] ListView: Attempt to insert exising section");
			}
		}];
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView insertSections:indexSet withRowAnimation:animation];
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)replaceSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger replaceIndex = [TiUtils intValue:[args objectAtIndex:0]];
	TiUIListSectionProxy *section = [args objectAtIndex:1];
	ENSURE_TYPE(section, TiUIListSectionProxy);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[self rememberProxy:section];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections containsObject:section]) {
			DebugLog(@"[WARN] ListView: Attempt to insert exising section");
			return;
		}
		if ([_sections count] <= replaceIndex) {
			DebugLog(@"[WARN] ListView: Replace section index is out of range");
			[self forgetProxy:section];
			return;
		}
		TiUIListSectionProxy *prevSection = [_sections objectAtIndex:replaceIndex];
		prevSection.delegate = nil;
		[_sections replaceObjectAtIndex:replaceIndex withObject:section];
		section.delegate = self;
		section.sectionIndex = replaceIndex;
		NSIndexSet *indexSet = [NSIndexSet indexSetWithIndex:replaceIndex];
		[tableView deleteSections:indexSet withRowAnimation:animation];
		[tableView insertSections:indexSet withRowAnimation:animation];
		[self forgetProxy:prevSection];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)scrollToItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
        UITableViewScrollPosition scrollPosition = [TiUtils intValue:@"position" properties:properties def:UITableViewScrollPositionNone];
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] ListView: Scroll to section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
            [self.listView.tableView scrollToRowAtIndexPath:indexPath atScrollPosition:scrollPosition animated:animated];
        }, NO);
    }
}

- (id)getItem:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
    if ([_sections count] <= sectionIndex) {
        DebugLog(@"[WARN] ListView: getItem section  index is out of range");
        return nil;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] ListView: getItem index is out of range");
        return nil;
    }
    return [section itemAtIndex:itemIndex];
}

- (id)getChildByBindId:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
	NSString *bindId = [TiUtils stringValue:[args objectAtIndex:2]];
    if ([_sections count] <= sectionIndex) {
        DebugLog(@"[WARN] ListView:getChildByBindId section index is out of range");
        return nil;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] ListView: getChildByBindId index is out of range");
        return nil;
    }
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
    TiUIListItem *cell = (TiUIListItem *)[self.listView.tableView cellForRowAtIndexPath:indexPath];
    id bindObject = [[cell proxy] valueForUndefinedKey:bindId];
    return bindObject;
}

-(void)scrollToTop:(id)args
{
	ENSURE_UI_THREAD(scrollToTop,args);
	NSInteger top = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *options = [args count] > 1 ? [args objectAtIndex:1] : nil;
	BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
	
	[self.listView scrollToTop:top animated:animated];
}

-(void)scrollToBottom:(id)args
{
	ENSURE_UI_THREAD(scrollToBottom,args);
	NSInteger top = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *options = [args count] > 1 ? [args objectAtIndex:1] : nil;
	BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
	
	[self.listView scrollToBottom:top animated:animated];
}

- (void)selectItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] ListView: Select section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] ListView: Select item index is out of range");
                return;
            }
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:itemIndex inSection:sectionIndex];
            [self.listView.tableView selectRowAtIndexPath:indexPath animated:YES scrollPosition:UITableViewScrollPositionNone];
            [self.listView.tableView scrollToRowAtIndexPath:indexPath atScrollPosition:UITableViewScrollPositionNone animated:YES];
        }, NO);
    }
}

- (void)deselectItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] ListView: Select section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] ListView: Select item index is out of range");
                return;
            }
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:itemIndex inSection:sectionIndex];
            [self.listView.tableView deselectRowAtIndexPath:indexPath animated:YES];
        }, NO);
    }
}

-(void)setContentInsets:(id)args
{
    id arg1;
    id arg2;
    if ([args isKindOfClass:[NSDictionary class]]) {
        arg1 = args;
        arg2 = nil;
    }
    else {
        arg1 = [args objectAtIndex:0];
        arg2 = [args count] > 1 ? [args objectAtIndex:1] : nil;
    }
    TiThreadPerformOnMainThread(^{
        [self.listView setContentInsets_:arg1 withObject:arg2];
    }, NO);
}

- (TiUIListSectionProxy *)getSectionAt:(id)args
{
    NSNumber *sectionIndex = nil;
	ENSURE_ARG_AT_INDEX(sectionIndex, args, 0, NSNumber);
	return [_sections objectAtIndex:[sectionIndex integerValue]];
}


- (TiUIListSectionProxy *)getItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
    TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        return [section getItemAt:[NSArray arrayWithObject:[args objectAtIndex:1]]];
    }
    else {
        DebugLog(@"[WARN] getItemAt item index is out of range");
    }
}

- (void)appendItems:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section appendItems:[NSArray arrayWithObject:[args objectAtIndex:1]]];
    }
    else {
        DebugLog(@"[WARN] appendItems:section item index is out of range");
    }
}

- (void)insertItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section insertItemsAt:[NSArray arrayWithObjects:[args objectAtIndex:1], [args objectAtIndex:2], nil]];
    }
    else {
        DebugLog(@"[WARN] insertItemsAt item index is out of range");
    }
}

- (void)replaceItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 4);
	TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section replaceItemsAt:[NSArray arrayWithObjects:[args objectAtIndex:1], [args objectAtIndex:2], [args objectAtIndex:3], nil]];
    }
    else {
        DebugLog(@"[WARN] replaceItemsAt item index is out of range");
    }
}

- (void)deleteItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section deleteItemsAt:[NSArray arrayWithObjects:[args objectAtIndex:1], [args objectAtIndex:2], nil]];
    }
    else {
        DebugLog(@"[WARN] deleteItemsAt item index is out of range");
    }
}

- (void)updateItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUIListSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section updateItemAt:[NSArray arrayWithObjects:[args objectAtIndex:1], [args objectAtIndex:2], nil]];
    }
    else {
        DebugLog(@"[WARN] updateItemAt item index is out of range");
    }
}

-(void)showPullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(showPullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

-(void)closePullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(closePullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

#pragma mark - Marker Support
- (void)setMarker:(id)args;
{
    ENSURE_SINGLE_ARG(args, NSDictionary);
    pthread_rwlock_wrlock(&_markerLock);
    int section = [TiUtils intValue:[args objectForKey:@"sectionIndex"] def:NSIntegerMax];
    int row = [TiUtils intValue:[args objectForKey:@"itemIndex"] def:NSIntegerMax];
    RELEASE_TO_NIL(marker);
    marker = [[NSIndexPath indexPathForRow:row inSection:section] retain];
    pthread_rwlock_unlock(&_markerLock);
}

-(void)willDisplayCell:(NSIndexPath*)indexPath
{
    if ((marker != nil) && [self _hasListeners:@"marker"]) {
        //Never block the UI thread
        int result = pthread_rwlock_tryrdlock(&_markerLock);
        if (result != 0) {
            return;
        }
        if ( (indexPath.section > marker.section) || ( (marker.section == indexPath.section) && (indexPath.row >= marker.row) ) ){
            [self fireEvent:@"marker" withObject:nil propagate:NO checkForListener:NO];
            RELEASE_TO_NIL(marker);
        }
        pthread_rwlock_unlock(&_markerLock);
    }
}

DEFINE_DEF_BOOL_PROP(willScrollOnStatusTap,YES);

@end

#endif
