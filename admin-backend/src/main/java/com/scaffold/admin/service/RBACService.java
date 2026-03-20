package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.UserPermissionOverrideDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.vo.*;
import java.util.List;
import java.util.Set;

public interface RBACService {

    // 用户-角色关联
    List<AdminRole> getUserRoles(Long userId);

    Set<Long> getUserRoleIds(Long userId);

    Set<String> getUserRoleCodes(Long userId);

    boolean hasRole(Long userId, String roleCode);

    void assignUserRoles(Long userId, List<Long> roleIds);

    void removeUserRoles(Long userId, List<Long> roleIds);

    void syncUserRoles(Long userId, List<Long> roleIds);

    // 用户权限覆盖
    void setUserPermissionOverride(Long userId, UserPermissionOverrideDTO dto);

    void removeUserPermissionOverride(Long userId, Long overrideId);

    void clearUserPermissionOverrides(Long userId);

    List<PermissionOverrideVO> getUserPermissionOverrides(Long userId);

    // 用户权限计算
    Set<Long> getUserPermissionIds(Long userId);

    UserPermissionVO getUserPermissionsDetail(Long userId);

    UserAvailablePermissionVO getUserAvailablePermissions(Long userId);

    UserEffectivePermissionVO getUserEffectivePermissions(Long userId);

    // 权限检查
    boolean checkPermission(Long userId, String path, String method);
}
