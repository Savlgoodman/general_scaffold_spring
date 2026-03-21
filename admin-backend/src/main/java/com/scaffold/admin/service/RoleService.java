package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.CreateRoleDTO;
import com.scaffold.admin.model.dto.RolePermissionDTO;
import com.scaffold.admin.model.dto.UpdateRoleDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.vo.RoleAssignablePermissionVO;
import com.scaffold.admin.model.vo.RolePermissionVO;
import java.util.List;

public interface RoleService {

    AdminRole getById(Long id);

    AdminRole getByCode(String code);

    List<AdminRole> getAll();

    List<AdminRole> getActiveRoles();

    AdminRole createRole(CreateRoleDTO dto);

    AdminRole updateRole(Long id, UpdateRoleDTO dto);

    void deleteRole(Long id);

    void deleteRoles(List<Long> ids);

    boolean isCodeExists(String code);

    void assignPermissions(Long roleId, List<RolePermissionDTO> permissions);

    void removePermissions(Long roleId, List<Long> permissionIds);

    void revokePermissions(Long roleId, List<Long> permissionIds);

    List<Long> getRolePermissionIds(Long roleId);

    RolePermissionVO getRolePermissionsDetail(Long roleId);

    RoleAssignablePermissionVO getRoleAssignablePermissions(Long roleId);
}
