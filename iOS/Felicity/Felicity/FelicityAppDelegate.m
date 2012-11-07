//
//  FelicityAppDelegate.m
//  Felicity
//
//  Naar deze klasse worden alle UI-interacties gedelegeerd.
//  Deze klasse is ook verantwoordelijk voor de informatie
//  omtrent de emoticons (source naam, aantal maal geselecteerd).
//
//  Created by Stijn Adams on 16/10/12.
//  Copyright (c) 2012 Ariadne. All rights reserved.
//

#import <CoreLocation/CoreLocation.h>
#import <EventKit/EventKit.h>
#import "FelicityAppDelegate.h"
#import "Emotion.h"
#import "Database.h"
#import "FMDatabase.h"

@implementation FelicityAppDelegate

@synthesize emotionsCount;
@synthesize emotions;

/*
** Deze methode wordt opgeroepen nadat de applicatie geladen is.
** Ze initialiseert de Array imagenames met de source name van elke emoticon.
** Bovendien zet deze methode voor elke emoticion het aantal maal
** geselecteerd terug op 0.
**
*/
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    
    /*
     * Location
     */
    
    if ([CLLocationManager locationServicesEnabled]== NO) {
        UIAlertView *servicesDisabledAlert = [[UIAlertView alloc] initWithTitle:@"Location Services Disabled" message:@"You currently have all location services for this device disabled. If you proceed, you will be asked to confirm whether location services should be reenabled." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [servicesDisabledAlert show];
    }
    
    locationController = [[MyCLController alloc] init];
    [locationController.locationManager startUpdatingLocation];
    
    
    /*
     * Fill database
     */
    
    NSArray *imageNames = [NSArray arrayWithObjects:@"angry", @"ashamed", @"bored", @"happy", @"hungry", @"in_love",@"irritated",@"sad", @"scared", @"sick", @"tired", @"very_happy", @"very_sad", @"super_happy", nil];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docsPath = [paths objectAtIndex:0];
    NSString *path = [docsPath stringByAppendingPathComponent:@"database.sqlite"];
    
    FMDatabase *db = [FMDatabase databaseWithPath:path];
    if (![db open]) {
        return NO;
    }
    [db executeUpdate:@"create table emotions (displayName text ,uniqueId int primary key,name text ,smallImage text,largeImage text,nbSelected int)"];
    
    
    for (int i = 0; i < imageNames.count; i++) {
        NSString *name =  imageNames[i];
        NSString *displayName = [[name stringByReplacingOccurrencesOfString:@"_" withString:@" "] capitalizedString];
        NSString *smallImage = [name stringByAppendingString:@"_small.png"];
        NSString *largeImage = [name stringByAppendingString:@"_big.png"];
        //Emotion *emo = [[Emotion alloc] initWithDisplayName:displayName
        //                                 andUniqueId:i
        //                             AndDatabaseName:name
        //                               AndSmallImage:smallImage
        //                               AndLargeImage:largeImage
        //                           AndNbSelected:0];
        [db executeUpdate:@"insert into emotions(displayName, uniqueId,name, smallImage, largeImage, nbSelected) values(?,?,?,?,?,?)",displayName,[NSNumber numberWithInt:i],name, smallImage, largeImage,[NSNumber numberWithInt:0],nil];
        //[[Database database] insertEmotion:emo];
    }
    
    [db executeUpdate:@"UPDATE emotions SET nbSelected=? WHERE name=?", [NSNumber numberWithInt:2], @"happy",nil];

    
    FMResultSet *results = [db executeQuery:@"select * from emotions"];
    while([results next]) {
        NSString *name = [results stringForColumn:@"name"];
        NSInteger nbSelected  = [results intForColumn:@"nbSelected"];
        NSLog(@"User: %@ - %d",name, nbSelected);
    }
    [db close];
    
  //  NSArray *emotionInformation = [[Database database] retrieveEmotionsFromDatabase];
  //  for (Emotion *info in emotionInformation) {
  //      [[Database database] incrementCountOfEmotion:info];
  //      NSLog(@"%d: %@, %@, %@, %d", info.uniqueId, info.databaseName, info.smallImage, info.largeImage, info.nbSelected);
  //  }
    
    // Let fmdb do the work
    
    emotionsCount = [[NSMutableDictionary alloc] init];
    for (NSInteger i = 0; i < imageNames.count; i++) {
        [emotionsCount setObject:[NSNumber numberWithInteger:0] forKey:imageNames[i]];
    }
            
    return YES;
}
							
- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.    
}

- (void)applicationWillTerminate:(UIApplication *)application
{

}

@end
