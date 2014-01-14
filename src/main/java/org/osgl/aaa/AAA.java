package org.osgl.aaa;

import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Collection;
import java.util.Map;

/**
 * Created by luog on 8/01/14.
 */
public enum  AAA {
    ;

    /**
     * The recommended name of system principal
     */
    public static final String SYSTEM = "__sys";

    /**
     * The recommended super user privilege level
     */
    public static final int SUPER_USER = 9999;

    private static final ThreadLocal<Object> target = new ThreadLocal<Object>();

    private static final Map<Class, DynamicPermissionCheckHelper> dynamicCheckers = C.newMap();

    private static final ThreadLocal<AAAContext> context = new ThreadLocal<AAAContext>();

    /**
     * Store a guarded resource to the thread local
     *
     * @param tgt the object to be guarded
     */
    public static void setTarget(Object tgt) {
        target.set(tgt);
    }

    private static Object target() {
        return target.get();
    }

    /**
     * Clear the guarded resource from the thread local
     */
    public static void clearTarget() {
        target.remove();
    }

    /**
     * Set AAAContext to thread local
     *
     * @param context
     */
    public static void setContext(AAAContext context) {
        if (null == context) AAA.context.remove();
        else AAA.context.set(context);
    }

    /**
     * Clear AAAContext thread local
     */
    public static void clearContext() {
        AAA.context.remove();
    }

    private static AAAContext context() {
        return context.get();
    }

    public static <T> void registerDynamicPermissionChecker(DynamicPermissionCheckHelper<T> checker, Class<T> clz) {
        dynamicCheckers.put(clz, checker);
    }

    /**
     * Check if a user has access to a guarded resource
     *
     * @param user
     * @param guarded
     * @param context
     * @return {@code true} if the user has access to the resource or {@code false} otherwise
     */
    public static boolean hasAccessTo(Principal user, Guarded guarded, AAAContext context) {
        E.NPE(user, guarded, context);
        AuthorizationService auth = context.getAuthorizationService();
        Privilege prU = auth.getPrivilege(user, context);
        Privilege prG = guarded.getPrivilege();
        if (null != prU && null != prG) {
            if (prU.getLevel() >= prG.getLevel()) return true;
        }

        Permission peG = guarded.getPermission();
        if (null == peG) {
            return false;
        }

        Collection<Permission> perms = auth.getAllPermissions(user, context);
        boolean hasAccess = perms.contains(peG);

        if (!hasAccess) return false;
        if (!peG.isDynamic()) return true;

        Object o = context.getGuardedTarget();
        E.NPE(o);
        Class<?> c = o.getClass();
        DynamicPermissionCheckHelper dc = dynamicCheckers.get(c);
        if (null == dc) {
            Class[] classes = c.getClasses();
            for (Class c0: classes) {
                dc = dynamicCheckers.get(c0);
                if (null != dc) break;
            }
        }
        if (null == dc) return false;
        return dc.isAssociated(o, user);
    }

    private static void noAccess() {
        throw new NoAccessException();
    }

    private static void noAccess(String reason) {
        throw new NoAccessException(reason);
    }

    public static boolean hasPermission(Object target, String permName, boolean allowSystem) {
        AAAContext context = context();
        if (null == context) noAccess("AAA context not found");
        Principal user = context.getCurrentPrincipal();
        if (null == user) {
            if (!allowSystem) noAccess("Cannot find current principal");
            user = context.getSystemPrincipal();
        }
        AAAPersistentService db = context.getPersistentService();
        Permission perm = db.findByName(permName, Permission.class);
        if (null == perm) return false;

        if (null == target) {
            return hasAccessTo(user, Guarded.Factory.byPermission(perm), context);
        }
        Object prevTarget = context.getGuardedTarget();
        context.setGuardedTarget(target);
        try {
            return hasAccessTo(user, Guarded.Factory.byPermission(perm), context);
        } finally {
            context.setGuardedTarget(prevTarget);
        }
    }

    /**
     * Authorize by permission. {@link org.osgl.aaa.AllowSystemAccount system account is allowed}
     * @param perm the permission name
     * @throws NoAccessException if the {@link AAAContext#getCurrentPrincipal() current principal}
     *                           does not have required permission
     */
    public static void requirePermission(String perm) throws NoAccessException {
        requirePermission(null, perm, true);
    }

    public static void requirePermission(String perm, boolean allowSystem) throws NoAccessException {
        requirePermission(null, perm, allowSystem);
    }

    public static void requirePermission(Object target, String perm) throws NoAccessException {
        requirePermission(target, perm, true);
    }

    public static void requirePermission(Object target, String permName, boolean allowSystem) {
        if (!hasPermission(target, permName, allowSystem)) noAccess();
    }

    public static boolean hasPrivilege(Object target, String privName, boolean allowSystem) {
        AAAContext context = context();
        if (null == context) noAccess("AAA context not found");
        Principal user = context.getCurrentPrincipal();
        if (null == user) {
            if (!allowSystem) noAccess("Cannot find current principal");
            user = context.getSystemPrincipal();
        }
        AAAPersistentService db = context.getPersistentService();
        Privilege priv = db.findByName(privName, Privilege.class);
        if (null == priv) return false;
        AuthorizationService auth = context.getAuthorizationService();
        Privilege userPriv = auth.getPrivilege(user, context);
        if (null == userPriv) return false;
        return userPriv.getLevel() >= priv.getLevel();
    }

    public static void requirePrivilege(Object target, String privName, boolean allowSystem) {
        if (!hasPrivilege(target, privName, allowSystem)) noAccess();
    }

    public static void requirePrivilege(Object target, String privName) {
        requirePrivilege(target, privName, true);
    }

    public static void requirePrivilege(String privName, boolean allowSystem) {
        requirePrivilege(null, privName, allowSystem);
    }

    public static void requirePrivilege(String privName) {
        requirePrivilege(null, privName, true);
    }

    private static boolean hasPermissionOrPrivilege(Object target, String permission, String privilege, boolean allowSystem) {
        AAAContext context = context();
        if (null == context) noAccess("AAA context not found");
        Principal user = context.getCurrentPrincipal();
        if (null == user) {
            if (!allowSystem) noAccess("Cannot find current principal");
            user = context.getSystemPrincipal();
        }
        AAAPersistentService db = context.getPersistentService();
        Privilege priv = db.findByName(privilege, Privilege.class);
        AuthorizationService auth = context.getAuthorizationService();
        if (null != priv) {
            Privilege userPriv = auth.getPrivilege(user, context);
            if (userPriv.getLevel() >= priv.getLevel()) return true;
        }

        Permission perm = db.findByName(permission, Permission.class);
        if (null == perm) {
            return false;
        }

        if (null == target) {
            return hasAccessTo(user, Guarded.Factory.byPermission(perm), context);
        }
        Object prevTarget = context.getGuardedTarget();
        context.setGuardedTarget(target);
        try {
            return hasAccessTo(user, Guarded.Factory.byPermission(perm), context);
        } finally {
            context.setGuardedTarget(prevTarget);
        }
    }

    public static void requirePermissionOrPrivilege(Object target, String permission, String privilege, boolean allowSystem) {
        if (!hasPermissionOrPrivilege(target, permission, privilege, allowSystem)) noAccess();
    }

    public static void requirePermissionOrPrivilege(String permission, String privilege) {
        requirePermissionOrPrivilege(null, permission, privilege, true);
    }

    public static void requirePermissionOrPrivilege(Object target, String permission, String privilege) {
        requirePermissionOrPrivilege(target, permission, privilege, true);
    }

    public static void requirePermissionOrPrivilege(String permission, String privilege, boolean allowSystem) {
        requirePermissionOrPrivilege(null, permission, privilege, allowSystem);
    }
}