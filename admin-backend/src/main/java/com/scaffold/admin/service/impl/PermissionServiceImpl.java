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

        // 处理 ** 通配符（匹配当前路径及所有子路径）
        // /api/admin/users/** 应匹配:
        //   /api/admin/users
        //   /api/admin/users/
        //   /api/admin/users/123
        //   /api/admin/users/123/roles
        if (pattern.contains("**")) {
            // 提取 ** 之前的前缀（如 /api/admin/users/）
            int dsIndex = pattern.indexOf("**");
            String prefix = pattern.substring(0, dsIndex);
            // 去掉末尾斜杠用于前缀匹配
            String prefixNoSlash = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;

            // 精确匹配前缀本身（如 /api/admin/users）
            if (path.equals(prefixNoSlash)) {
                return true;
            }
            // 匹配前缀下的所有子路径（如 /api/admin/users/xxx）
            if (path.startsWith(prefix)) {
                return true;
            }
            return false;
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

    @Override
    public int calculateSpecificity(AdminPermission permission, String requestPath, String requestMethod) {
        if (permission == null || requestPath == null) {
            return 0;
        }

        int specificity = 0;

        // 基础 specificity 基于路径匹配模式
        int pathSpecificity = calculatePathSpecificity(permission.getPath(), requestPath);
        specificity += pathSpecificity;

        // HTTP方法的特异性：精确方法 > *通配符
        if (permission.getMethod() != null) {
            if (permission.getMethod().equalsIgnoreCase(requestMethod)) {
                specificity += 100;  // 精确方法匹配
            } else if (permission.getMethod().equals("*")) {
                specificity += 50;   // 通配符方法
            }
        }

        // 组权限 vs 子权限：子权限优先级更高
        if (permission.getIsGroup() == null || permission.getIsGroup() == 0) {
            specificity += 10;  // 子权限
        } else {
            specificity += 5;   // 组权限
        }

        return specificity;
    }

    @Override
    public int calculatePathSpecificity(String pattern, String path) {
        if (pattern == null || path == null) {
            return 0;
        }

        // 精确匹配
        if (pattern.equals(path)) {
            return 1000;
        }

        // 处理通配符匹配
        boolean hasDoubleWildcard = pattern.contains("**");
        boolean hasSingleWildcard = pattern.contains("*") || pattern.contains("?");
        boolean hasSingleStarOnly = pattern.matches("[^*]*\\*[^/]*");  // 只包含单个 * 不包含 **

        if (hasDoubleWildcard) {
            // 双通配符匹配，优先级最低
            // 检查是否真正匹配
            if (matchPattern(pattern, path)) {
                return 100;
            }
        } else if (hasSingleWildcard) {
            // 单通配符匹配
            if (matchPattern(pattern, path)) {
                // 单 * 的 specificity 取决于替换后与实际路径的接近程度
                if (hasSingleStarOnly) {
                    return 500;  // 单通配符，优先级高于双通配符
                }
                return 300;
            }
        } else {
            // 无通配符但也不精确匹配，可能是路径层次差异
            // 尝试计算层级相似度
            String[] patternParts = pattern.split("/");
            String[] pathParts = path.split("/");

            if (patternParts.length > 0 && pathParts.length > 0) {
                // 计算前缀匹配的层级数
                int matchLevel = 0;
                int minLen = Math.min(patternParts.length, pathParts.length);
                for (int i = 0; i < minLen; i++) {
                    if (patternParts[i].equals(pathParts[i])) {
                        matchLevel++;
                    } else {
                        break;
                    }
                }
                // 基于匹配层级的特异性
                return matchLevel * 50;
            }
        }

        return 0;
    }

}
