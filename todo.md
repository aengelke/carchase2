# TODO for CarChase 2

## Milestone 1
 * [x] Split up in files for messages, world representation and configuration
 * [x] Implement Trigger lines for TTS

## Milestone 2
 * [x] Implement speed changes
 * [x] Implement recreation of drive action
   * [x] Implement recreation of timeline and get interruption to work
 * [x] Improve TTS stability

## Milestone 3
 * [x] Implement Control possiblitiesS+
   * [x] Use `0` ... for path and `+-` for speed
   * [x] Use 4 speed steps
 * [x] Implement MessageInformationLevel and MessageType
 * [x] If best matching info level is not available, use nearest one
 * [x] Add interface for adding other situations
 * [x] Use another Thread for matching situations
 * [ ] Remove configuration file completely
   * [ ] Implement dialog to choose start point
   * [ ] Implement possibility to set car with distance to a points

## Milestone 4
 * [ ] Add other situations
 * [ ] Use another map
   * [ ] Implement zooming on a bigger map
 * [ ] Implement spoke in last seconds function
   * [ ] How to get the absolute end time of an IU? Ask Timo
 * [ ] Add speed changes to the configuration file for automated testing

## No Milestone
 * [x] Use Processing's `PApplet` instead of `JPanel`
   * [x] Get rid of SwingRepaintTimeline
   * [ ] Make sure that all variables are not null on run
 * [ ] Improve rendering of the car in a curve
 * [ ] Add point-of-view perspective
   * [ ] Switch to OpenGL
 * [ ] Save street history and not just the previous street
   * [ ] Implement this in the configuration and remove other occourences
