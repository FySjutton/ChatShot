plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.6-fabric" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
    group = "project"
    ofTask("build")
}
