CarChase 2
==========

Source code repository for the CarChase 2 simulator based on InproTK.

![CarChase 2](https://cloud.githubusercontent.com/assets/4236689/4061449/3ba39dd6-2df8-11e4-926f-77ad451f5440.png)

### Requirements
 * Java 7
 * Ant
 * MaryTTS

### Run instructions
 * Change path to MaryTTS in the `build.xml` file
 * Run `ant carchase2` from the command line

---

### Known Bugs
 * Auto-Revoke sometimes doesn't work (InproTK bug)
 * Synthesis quality of latest InproTK/develop/HEAD is way too bad

### Other Issues and Ideas
 * Sometimes two hesitations occour directly after each other
 * Implement spoke in last seconds function
 * Add point-of-view perspective (in 2D)
 * Save street history and not just the previous street
 * Extend existing map (or even use OSM data?)
 * Have the user select the starting point on the map