select meta_attr_value from IRODS.R_COLL_HIERARCHY_META_MAIN where OBJECT_ID IN(1346055,
1351868,
1346014,
1346001,
1346020,
1351872,
1351861,
1346007,
1346081
) and META_ID=931692;


/*on DEV:select OBJECT_ID,OBJECT_PATH,META_ATTR_VALUE from IRODS.R_COLL_HIERARCHY_META_MAIN where META_ID=1208984;*/

select distinct(COLLECTION_ID) from COLLECTION_UPDATE_PERMISSIONS_T;