package gov.nih.nci.doe.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import gov.nih.nci.doe.web.domain.MetaDataPermissions;

public interface MetaDataPermissionsRepository extends JpaRepository<MetaDataPermissions, String> {

	@Query("select a from MetaDataPermissions a where a.collectionId =?1")
    List<MetaDataPermissions> getAllMetaDataPermissionsByCollectionId(Integer collectionId);
}