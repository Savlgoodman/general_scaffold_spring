package com.scaffold.admin.service;

import com.scaffold.admin.model.entity.AdminPermission;
import com.scaffold.admin.model.vo.PermissionBaseVO;
import com.scaffold.admin.model.vo.PermissionGroupVO;
import java.util.List;

public interface PermissionService {

    AdminPermission getById(Long id);

    AdminPermission getByCode(String code);

    List<AdminPermission> getAll();

    List<AdminPermission> getByGroupKey(String groupKey);

    List<AdminPermission> getActivePermissions();

    boolean matchPattern(String pattern, String path);

    List<AdminPermission> findMatchingPermissions(String path, String method);

    boolean isGroupPermission(Long permissionId);

    List<AdminPermission> getChildPermissions(String groupKey);

    List<PermissionGroupVO> getAllGroupedPermissions();

    PermissionBaseVO convertToBaseVO(AdminPermission permission);
}
