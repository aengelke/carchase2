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
 * [x] Remove configuration file completely
   * [x] Implement dialog to choose start point

## Milestone 4
 * [x] be able to specify routes in advance (as in the old prototype)
   * [x] specify routes in advance via navigation on the board (something like a pause/play button) --> set speed to 0
 * [x] be able to specify multiple steps in advance via keyboard (maybe take back future steps with the slash key?)
 * [ ] Implement spoke in last seconds function
   * [ ] How to get the absolute end time of an IU? Ask Timo --> Timo tries to implement this
 * [x] Add speed changes to the configuration file for automated testing
 * [ ] Hesitations are buggy
   * [ ] However, sometimes two hesitations occour directly after each other
 * [ ] Duration of an IU is not correct
 * [ ] Auto-Revoke sometimes doesn't work
 * [ ] Take random choice if there are many options
 * [x] Implement Patterns.
   * [x] Define Specs for a `patterns.txt`
   * [x] Implement Parser for a `patterns.txt`
   * [x] Generate text at runtime, if no special message exists.
   * [ ] Implement other variables

## No Milestone
 * [x] Use Processing's `PApplet` instead of `JPanel`
   * [x] Get rid of SwingRepaintTimeline
   * [x] Make sure that all variables are not null on run
 * [ ] Use message with lower information level on revoke, if the distance is smaller than 4 seconds
   * [ ] Requires a correct duration of an IU
 * [ ] Improve rendering of the car in a curve
 * [ ] Add point-of-view perspective (in 2D)
 * [ ] Save street history and not just the previous street
   * [ ] Implement this in the configuration and remove other occourences
 * [ ] Extend existing map
 * [ ] Add other situations --> really needed?
 * [ ] have the user select the starting point with digits on the map? (0-9 points should always offer enough options)
