# Articulator

The articulator has the task of articulating (saying) a TTS action. Therefore,
it offers some Methods:

 * `say(TTSAction)`: Articulate a TTS action. If the text ends with `<hes>`,
     a hesitation will be added.
 * `isSpeaking()`: Returns whether the dispatcher is currently speaking
 * `getLast()`: Returns the last articulated or upcoming TTS action, or `null`
 * `getLastUcoming()`: Returns the last upcoming TTS action, or `null`
 * `autoRemoveUpcoming()`: Automatically removes optional TTS actions

Currently, there are two articulators implemented: A standard and an incremental
articulator.

## Standard Articulator
This articulator just dispatches the text to articulate, and has no support for
the incremental features, like `getLast` or `isSpeaking`. Therefore, it doesn't
support hesitations. It produces a text like this:

> The car is driving in the 6th avenue. The car turns left. The car slows down.

## Incremental Articulator
This articulator is the default articulator and can produce a text like this:

> The car is driving in the 6th avenue, it turns left and slows down.

The incremental articulator will support `spokeInLastSeconds(double)` to
determine whether the articulator spoke in the last time in order to append
continuations after a break. (This is not implemented yet.)
