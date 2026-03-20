package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.RolePermissionDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.vo.RoleAssignablePermissionVO;
import com.scaffold.admin.model.vo.RolePermissionVO;
import java.util.List;

public interface RoleService {

    AdminRole getById(Long id);

    AdminRole getByCode(String code);

    List<AdminRole> getAll();

    List<AdminRole> getActiveRoles();

    void assignPermissions(Long roleId, List<RolePermissionDTO> permissions);

    void removePermissions(Long roleId, List<Long> permissionIds);

    void revokePermissions(Long roleId, List<Long> permissionIds);

    List<Long> getRolePermissionIds(Long roleId);

    RolePermissionVO getRolePermissionsDetail(Long roleId);

    RoleAssignablePermissionVO getRoleAssignablePermissions(Long roleId);
}
