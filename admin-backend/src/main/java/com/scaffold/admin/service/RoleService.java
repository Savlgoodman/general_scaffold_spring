package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.CreateRoleDTO;
import com.scaffold.admin.model.dto.UpdateRoleDTO;
import com.scaffold.admin.model.entity.AdminRole;

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
}
