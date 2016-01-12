
function debounce(func, wait, immediate){
  var timeout, args, context, timestamp, result;
  if (null == wait) wait = 100;

  function later() {
    var last = now() - timestamp;

    if (last < wait && last > 0) {
      timeout = setTimeout(later, wait - last);
    } else {
      timeout = null;
      if (!immediate) {
        result = func.apply(context, args);
        if (!timeout) context = args = null;
      }
    }
  };

  return function debounced() {
    context = this;
    args = arguments;
    timestamp = now();
    var callNow = immediate && !timeout;
    if (!timeout) timeout = setTimeout(later, wait);
    if (callNow) {
      result = func.apply(context, args);
      context = args = null;
    }

    return result;
  };
}

var COLORS = {
		links: '#FAFAFA',
		text: {
			subtitle: '#FAFAFA'
		},
		nodes: {
			method: function (d, i) {
				return d.me
				? d3.hsl(210, 0.7, 0.725) // blue
						: d.seeder
						? d3.hsl(120, 0.7, 0.725) // green
								: d3.hsl(55, 0.7, 0.725) // yellow
			},
			hover: '#FAFAFA',
			dep: '#252929'
		}
}

function TorrentGraph (root) {
  if (typeof root === 'string') root = document.querySelector(root)
  var model = {
    nodes: [],
    links: []
  }

  function scale () {
    var len = model.nodes.length
    return len < 10
      ? 1
      : Math.max(0.2, 1 - ((len - 10) / 100))
  }

  var width 	= 300
  var height 	= 300

  var focus

  model.links.forEach(function (link) {
    var source = model.nodes[link.source]
    var target = model.nodes[link.target]

    source.children = source.children || []
    source.children.push(link.target)

    target.parents = target.parents || []
    target.parents.push(link.source)
  })

  var svg = d3.select(root).append('svg')
    .attr('width', width)
    .attr('height', height)

  var force = d3.layout.force()
    .size([width, height])
    .nodes(model.nodes)
    .links(model.links)
    .on('tick', function () {
      link
        .attr('x1', function (d) { return d.source.x })
        .attr('y1', function (d) { return d.source.y })
        .attr('x2', function (d) { return d.target.x })
        .attr('y2', function (d) { return d.target.y })

      node
        .attr('cx', function (d) { return d.x })
        .attr('cy', function (d) { return d.y })

      node.attr('transform', function (d) { return 'translate(' + d.x + ',' + d.y + ')' })
    })

  var node = svg.selectAll('.node')
  var link = svg.selectAll('.link')

  update()

  function update () {
    link = link.data(model.links)
    node = node.data(model.nodes, function (d) { return d.id })

    link.enter()
      .insert('line', '.node')
        .attr('class', 'link')
        .style('stroke', COLORS.links)
        .style('opacity', 0.5)

    link.exit()
      .remove()

    var g = node.enter()
      .append('g')
      .attr('class', 'node')

    g.call(force.drag)

    g.append('circle')
      .on('mouseover', function (d) {
        d3.select(this)
          .style('fill', COLORS.nodes.hover)

        d3.selectAll(childNodes(d))
          .style('fill', COLORS.nodes.hover)
          .style('stroke', COLORS.nodes.method)
          .style('stroke-width', 2)

        d3.selectAll(parentNodes(d))
          .style('fill', COLORS.nodes.dep)
          .style('stroke', COLORS.nodes.method)
          .style('stroke-width', 2)
      })
      .on('mouseout', function (d) {
        d3.select(this)
          .style('fill', COLORS.nodes.method)

        d3.selectAll(childNodes(d))
          .style('fill', COLORS.nodes.method)
          .style('stroke', null)

        d3.selectAll(parentNodes(d))
          .style('fill', COLORS.nodes.method)
          .style('stroke', null)
      })
      .on('click', function (d) {
        if (focus === d) {
          force.charge(-200 * scale())
            .linkDistance(100 * scale())
            .linkStrength(1)
            .start()

          node.style('opacity', 1)
          link.style('opacity', 0.3)

          focus = false

          return
        }

        focus = d

        node.style('opacity', function (o) {
          o.active = connected(d, o)
          return o.active ? 1 : 0.2
        })

        force.charge(function (o) {
          return (o.active ? -100 : -5) * scale()
        }).linkDistance(function (l) {
          return (l.source.active && l.target.active ? 100 : 20) * scale()
        }).linkStrength(function (l) {
          return (l.source === d || l.target === d ? 1 : 0) * scale()
        }).start()

        link.style('opacity', function (l, i) {
          return l.source.active && l.target.active ? 0.2 : 0.02
        })
      })

    node.select('circle')
      .attr('r', function (d) {
        return scale() * (d.me ? 15 : 10)
      })
      .style('fill', COLORS.nodes.method)

    g.append('text')
      .attr('class', 'text')
      .text(function (d) { return d.me ? 'You' : d.ip })

    node.select('text')
      .attr('font-size', function (d) {
        return d.me ? 16 * scale() : 12 * scale()
      })
      .attr('dx', 0)
      .attr('dy', function (d) {
        return d.me ? -22 * scale() : -15 * scale()
      }).style( 'fill', 'white')

    node.exit()
      .remove()

    force
      .linkDistance(100 * scale())
      .charge(-200 * scale())
      .start()
  }

  function refresh (e) {
    width = Math.max(root.offsetWidth * 0.4, 500)
    height = root.offsetHeight

    force
      .size([width, height])
      .resume()

    svg
      .attr('width', root.offsetWidth)
      .attr('height', height)
  }

  function childNodes (d) {
    if (!d.children) return []

    return d.children
      .map(function (child) {
        return node[0][child]
      }).filter(function (child) {
        return child
      })
  }

  function parentNodes (d) {
    if (!d.parents) return []

    return d.parents
      .map(function (parent) {
        return node[0][parent]
      }).filter(function (parent) {
        return parent
      })
  }

  function connected (d, o) {
    return o.id === d.id ||
    (d.children && d.children.indexOf(o.id) !== -1) ||
    (o.children && o.children.indexOf(d.id) !== -1) ||
    (o.parents && o.parents.indexOf(d.id) !== -1) ||
    (d.parents && d.parents.indexOf(o.id) !== -1)
  }

  function getNode (id) {
    for (var i = 0, len = model.nodes.length; i < len; i += 1) {
      var node = model.nodes[i]
      if (node.id === id) return node
    }
    return null
  }

  function getNodeIndex (id) {
    for (var i = 0, len = model.nodes.length; i < len; i += 1) {
      var node = model.nodes[i]
      if (node.id === id) return i
    }
    return -1
  }

  function getLink (source, target) {
    for (var i = 0, len = model.links.length; i < len; i += 1) {
      var link = model.links[i]
      if (link.source === model.nodes[source] && link.target === model.nodes[target]) {
        return link
      }
    }
    return null
  }

  function getLinkIndex (source, target) {
    for (var i = 0, len = model.links.length; i < len; i += 1) {
      var link = model.links[i]
      if (link.source === model.nodes[source] && link.target === model.nodes[target]) {
        return i
      }
    }
    return -1
  }

  function add (node) {
    debug('add %s %o', node.id, node)
    if (getNode(node.id)) throw new Error('add: cannot add duplicate node')
    model.nodes.push(node)
    update()
  }

  function remove (id) {
    debug('remove $s', id)
    var index = getNodeIndex(id)
    if (index === -1) throw new Error('remove: node does not exit')
    model.nodes.splice(index, 1)
    update()
  }

  function connect (sourceId, targetId) {
    debug('connect %s %s', sourceId, targetId)
    var sourceNode = getNode(sourceId)
    if (!sourceNode) throw new Error('connect: invalid source id')
    var targetNode = getNode(targetId)
    if (!targetNode) throw new Error('connect: invalid target id')
    if (getLink(sourceNode.index, targetNode.index)) {
      throw new Error('connect: cannot make duplicate connection')
    }
    model.links.push({ source: sourceNode.index, target: targetNode.index })
    update()
  }

  function disconnect (sourceId, targetId) {
    debug('disconnect %s %s', sourceId, targetId)
    var sourceNode = getNode(sourceId)
    if (!sourceNode) throw new Error('connect: invalid source id')
    var targetNode = getNode(targetId)
    if (!targetNode) throw new Error('connect: invalid target id')
    var index = getLinkIndex(sourceNode.index, targetNode.index)
    if (index === -1) throw new Error('disconnect: connection does not exist')
    model.links.splice(index, 1)
    update()
  }

  function unchoke (sourceId, targetId) {
    debug('unchoke %s %s', sourceId, targetId)
  }

  function choke (sourceId, targetId) {
    debug('choke %s %s', sourceId, targetId)
  }

  window.addEventListener('resize', debounce(refresh, 500))
  refresh()

  return {
    add: add,
    remove: remove,
    connect: connect,
    disconnect: disconnect,
    unchoke: unchoke,
    choke: choke
  }
}

