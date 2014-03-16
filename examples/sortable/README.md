draggable
===

This is the sugared version of the om draggable example.

## Notable differences:

* The sorting logic is a bit different, items transact a :top value, and the app sorts data by :top and assigns the index in state.
* The draggable component uses sug events, and sug/private! to store drag cycle data without triggering renders.
* The components display their render count
* some css transitions to make position swapping pretty

[live example] (http://www.selfsamegames/com/sug/examples/sortable)

