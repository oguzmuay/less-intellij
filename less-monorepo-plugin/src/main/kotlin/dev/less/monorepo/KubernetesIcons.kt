package dev.less.monorepo

import javax.swing.Icon

/** Kubernetes / Helm / Kustomize manifest icons, chosen by the resource kind named in a YAML file's name. */
object KubernetesIcons {

    private val exact: Map<String, Icon> = buildMap {
        put("chart", NamedIcons.load("helm-chart"))
        put("helmfile", NamedIcons.load("helm-chart"))
        put("kustomization", NamedIcons.load("k8s-kustomization"))
    }

    private val kinds: Map<String, Icon> = buildMap {
        fun add(icon: String, vararg names: String) = NamedIcons.load(icon).let { i -> names.forEach { put(it, i) } }
        add("k8s-deployment", "deployment", "deploy", "replicaset", "rs")
        add("k8s-pod", "pod")
        add("k8s-service", "service", "svc", "endpoint")
        add("k8s-ingress", "ingress", "ing", "httproute", "ingressroute")
        add("k8s-configmap", "configmap", "cm")
        add("k8s-secret", "secret", "sealedsecret", "externalsecret")
        add("k8s-statefulset", "statefulset", "sts")
        add("k8s-daemonset", "daemonset", "ds")
        add("k8s-job", "job")
        add("k8s-cronjob", "cronjob", "cj")
        add("k8s-namespace", "namespace", "ns")
        add("k8s-volume", "pvc", "pv", "persistentvolumeclaim", "persistentvolume", "storageclass", "volume")
        add("k8s-hpa", "hpa", "horizontalpodautoscaler", "autoscaler")
        add("k8s-rbac", "serviceaccount", "sa", "role", "rolebinding", "clusterrole", "clusterrolebinding", "rbac")
        add("k8s-networkpolicy", "networkpolicy", "netpol")
        add("helm-values", "values")
    }

    /**
     * Icon for a lower-cased YAML file name whose dash/dot/underscore-separated parts name a resource kind, or null.
     * Example: "api-svc.prod.yaml" -> service, "values-dev.yml" -> helm values, "Chart.yaml" -> helm chart, "app.yaml" -> null
     */
    fun forFile(lowerName: String): Icon? {
        val base = when {
            lowerName.endsWith(".yaml") -> lowerName.removeSuffix(".yaml")
            lowerName.endsWith(".yml") -> lowerName.removeSuffix(".yml")
            else -> return null
        }
        exact[base]?.let { return it }
        // shortcut: a Spring "user-service.yaml" also matches "service"; check the file content if that gets annoying.
        return base.split('-', '.', '_').firstNotNullOfOrNull { kinds[it] ?: kinds[it.removeSuffix("s")] }
    }
}
