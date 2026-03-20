package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.admin.mapper.AdminPermissionMapper;
import com.scaffold.admin.model.entity.AdminPermission;
import com.scaffold.admin.model.vo.PermissionBaseVO;
import com.scaffold.admin.model.vo.PermissionGroupVO;
import com.scaffold.admin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final AdminPermissionMapper permissionMapper;

    @Override
    public AdminPermission getById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public AdminPermission getByCode(String code) {
        return permissionMapper.selectOne(
            new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getCode, code)
                .eq(AdminPermission::getIsDeleted, 0)
        );
    }

    @Override
    public List<AdminPermission> getAll() {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getIsDeleted, 0)
                .orderByAsc(AdminPermission::getSort)
        );
    }

    @Override
    public List<AdminPermission> getByGroupKey(String groupKey) {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getGroupKey, groupKey)
                .eq(AdminPermission::getIsDeleted, 0)
                .orderByAsc(AdminPermission::getSort)
        );
    }

    @Override
    public List<AdminPermission> getActivePermissions() {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getIsDeleted, 0)
                .eq(AdminPermission::getStatus, 1)
                .orderByAsc(AdminPermission::getSort)
        );
    }

    @Override
    public boolean matchPattern(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }

        // 处理 ** 通配符（匹配多级路径）
        if (pattern.contains("**")) {
            String regex = pattern
                .replace("**", "<<<DS>>>")
                .replace("*", "[^/]*")
                .replace("?", "[^/]")
                .replace("<<<DS>>>", ".*");
            return Pattern.matches("^" + regex + "$", path);
        }

        // 处理单级通配符 * 和 ?
        if (pattern.contains("*") || pattern.contains("?")) {
            String regex = pattern
                .replace("*", "[^/]*")
                .replace("?", "[^/]");
            return Pattern.matches("^" + regex + "$", path);
        }

        // 精确匹配
        return pattern.equals(path);
    }

    @Override
    public List<AdminPermission> findMatchingPermissions(String path, String method) {
        List<AdminPermission> allActive = getActivePermissions();
        return allActive.stream()
            .filter(perm -> {
                // 检查方法匹配
                if (perm.getMethod() != null && !perm.getMethod().equals("*")
                    && !perm.getMethod().equalsIgnoreCase(method)) {
                    return false;
                }
                // 检查路径匹配
                return matchPattern(perm.getPath(), path);
            })
            .collect(Collectors.toList());
    }

    @Override
    public boolean isGroupPermission(Long permissionId) {
        AdminPermission permission = getById(permissionId);
        return permission != null && permission.getIsGroup() != null
            && permission.getIsGroup() == 1;
    }

    @Override
    public List<AdminPermission> getChildPermissions(String groupKey) {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getGroupKey, groupKey)
                .eq(AdminPermission::getIsGroup, 0)
                .eq(AdminPermission::getIsDeleted, 0)
                .orderByAsc(AdminPermission::getSort)
        );
    }

    @Override
    public List<PermissionGroupVO> getAllGroupedPermissions() {
        List<AdminPermission> allPermissions = getAll();
        Map<String, List<AdminPermission>> groupMap = new HashMap<>();
        List<AdminPermission> groupPermissions = new ArrayList<>();

        for (AdminPermission perm : allPermissions) {
            if (perm.getIsGroup() != null && perm.getIsGroup() == 1) {
                groupPermissions.add(perm);
            } else if (perm.getGroupKey() != null) {
                groupMap.computeIfAbsent(perm.getGroupKey(), k -> new ArrayList<>()).add(perm);
            }
        }

        List<PermissionGroupVO> result = new ArrayList<>();
        for (AdminPermission groupPerm : groupPermissions) {
            PermissionGroupVO groupVO = new PermissionGroupVO();
            groupVO.setGroupKey(groupPerm.getGroupKey());
            groupVO.setGroupName(groupPerm.getGroupName());
            groupVO.setGroupPermission(convertToBaseVO(groupPerm));

            List<AdminPermission> children = groupMap.getOrDefault(groupPerm.getGroupKey(), new ArrayList<>());
            groupVO.setChildren(children.stream()
                .map(this::convertToBaseVO)
                .collect(Collectors.toList()));
            groupVO.setTotalCount(children.size());

            result.add(groupVO);
        }

        return result;
    }

    @Override
    public PermissionBaseVO convertToBaseVO(AdminPermission permission) {
        if (permission == null) {
            return null;
        }
        PermissionBaseVO vo = new PermissionBaseVO();
        vo.setId(permission.getId());
        vo.setName(permission.getName());
        vo.setCode(permission.getCode());
        vo.setPath(permission.getPath());
        vo.setMethod(permission.getMethod());
        vo.setIsGroup(permission.getIsGroup() != null && permission.getIsGroup() == 1);
        vo.setGroupKey(permission.getGroupKey());
        vo.setGroupName(permission.getGroupName());
        vo.setDescription(permission.getDescription());
        vo.setStatus(permission.getStatus());
        vo.setSort(permission.getSort());
        return vo;
    }
}
