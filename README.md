# die

Diff it exceptionally, dramatic in excellence tree-node diffing api!

## Examples

Look at `src/test/java/javaparser/JavaParserTest.java`

## Network protocol (over HTTP)

```
constant_pool_index: int
tree_id: int
```

Request
```
tree_id original
tree_id modified

tree original
tree modified

tree x:
  constant_pool_index type x
  constant_pool_index label x
  int size children x

  for child of children x:
    tree_id child
    tree child

int size constant_pool

for entry of constant_pool:
  string entry
```

Response
```
int size matches
int size actions

for match of matches:
  tree_id from match
  tree_id to match

for action of actions:
  match action:
    delete:
      byte 0
      tree_id node
    insert:
      byte 1
      tree_id node
      tree_id parent
      int pos
    move:
      byte 2
      tree_id node
      tree_id parent
      int pos
    replace:
      byte 3
      tree_id node
      tree_id x
```

## License

MIT license
