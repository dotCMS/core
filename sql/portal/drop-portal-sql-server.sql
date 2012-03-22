if exists (select * from dbo.sysobjects where id = object_id(N'[ABContact]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ABContact]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ABContacts_ABLists]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ABContacts_ABLists]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ABList]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ABList]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Address]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Address]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[AdminConfig]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [AdminConfig]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BJEntries_BJTopics]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BJEntries_BJTopics]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BJEntries_BJVerses]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BJEntries_BJVerses]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BJEntry]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BJEntry]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BJTopic]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BJTopic]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BJVerse]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BJVerse]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsCategory]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsCategory]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsComments]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsComments]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsEntry]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsEntry]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsLink]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsLink]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsProps]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsProps]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsReferer]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsReferer]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BlogsUser]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BlogsUser]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BookmarksEntry]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BookmarksEntry]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[BookmarksFolder]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [BookmarksFolder]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[CalEvent]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [CalEvent]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[CalTask]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [CalTask]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Company]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Company]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Counter]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Counter]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[CyrusUser]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [CyrusUser]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[CyrusVirtual]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [CyrusVirtual]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[DLFileProfile]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [DLFileProfile]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[DLFileRank]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [DLFileRank]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[DLFileVersion]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [DLFileVersion]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[DLRepository]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [DLRepository]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Group_]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Group_]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Groups_Roles]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Groups_Roles]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[IGFolder]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [IGFolder]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[IGImage]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [IGImage]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Image]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Image]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[JournalArticle]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [JournalArticle]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[JournalStructure]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [JournalStructure]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[JournalTemplate]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [JournalTemplate]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Layer]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Layer]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Layout]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Layout]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[MBMessage]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [MBMessage]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[MBMessageFlag]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [MBMessageFlag]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[MBThread]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [MBThread]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[MBTopic]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [MBTopic]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[MailReceipt]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [MailReceipt]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[NetworkAddress]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [NetworkAddress]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Note]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Note]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PasswordTracker]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PasswordTracker]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PollsChoice]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PollsChoice]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PollsDisplay]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PollsDisplay]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PollsQuestion]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PollsQuestion]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PollsVote]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PollsVote]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Portlet]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Portlet]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[PortletPreferences]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [PortletPreferences]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ProjFirm]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ProjFirm]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ProjProject]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ProjProject]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ProjTask]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ProjTask]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ProjTime]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ProjTime]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Release_]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Release_]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Role_]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Role_]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingCart]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingCart]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingCategory]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingCategory]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingCoupon]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingCoupon]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingItem]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingItem]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingItemField]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingItemField]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingItemPrice]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingItemPrice]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingOrder]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingOrder]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[ShoppingOrderItem]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [ShoppingOrderItem]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Skin]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Skin]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[UserTracker]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [UserTracker]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[UserTrackerPath]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [UserTrackerPath]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[User_]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [User_]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Users_Groups]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Users_Groups]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Users_ProjProjects]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Users_ProjProjects]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Users_ProjTasks]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Users_ProjTasks]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[Users_Roles]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [Users_Roles]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[WikiDisplay]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [WikiDisplay]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[WikiNode]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [WikiNode]
GO

if exists (select * from dbo.sysobjects where id = object_id(N'[WikiPage]') and OBJECTPROPERTY(id, N'IsUserTable') = 1)
drop table [WikiPage]
GO
