// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

operator fun Int.invoke() = this

fun foo(arg: Int?) {
    <caret>arg()
}