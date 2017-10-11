// See LICENSE.SiFive for license details.

package freechips.rocketchip.amba.axi4

import Chisel._
import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._

object AXI4Imp extends NodeImp[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4EdgeParameters, AXI4EdgeParameters, AXI4Bundle]
{
  def edgeO(pd: AXI4MasterPortParameters, pu: AXI4SlavePortParameters): AXI4EdgeParameters = AXI4EdgeParameters(pd, pu)
  def edgeI(pd: AXI4MasterPortParameters, pu: AXI4SlavePortParameters): AXI4EdgeParameters = AXI4EdgeParameters(pd, pu)

  def bundleO(eo: AXI4EdgeParameters): AXI4Bundle = AXI4Bundle(eo.bundle)
  def bundleI(ei: AXI4EdgeParameters): AXI4Bundle = AXI4Bundle(ei.bundle)

  def colour = "#00ccff" // bluish
  override def labelI(ei: AXI4EdgeParameters) = (ei.slave.beatBytes * 8).toString
  override def labelO(eo: AXI4EdgeParameters) = (eo.slave.beatBytes * 8).toString

  override def mixO(pd: AXI4MasterPortParameters, node: OutwardNode[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4Bundle]): AXI4MasterPortParameters  =
   pd.copy(masters = pd.masters.map  { c => c.copy (nodePath = node +: c.nodePath) })
  override def mixI(pu: AXI4SlavePortParameters, node: InwardNode[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4Bundle]): AXI4SlavePortParameters =
   pu.copy(slaves  = pu.slaves.map { m => m.copy (nodePath = node +: m.nodePath) })
}

// Nodes implemented inside modules
case class AXI4IdentityNode() extends IdentityNode(AXI4Imp)
case class AXI4MasterNode(portParams: Seq[AXI4MasterPortParameters]) extends SourceNode(AXI4Imp)(portParams)
case class AXI4SlaveNode(portParams: Seq[AXI4SlavePortParameters]) extends SinkNode(AXI4Imp)(portParams)
case class AXI4AdapterNode(
  masterFn:  AXI4MasterPortParameters => AXI4MasterPortParameters,
  slaveFn:   AXI4SlavePortParameters  => AXI4SlavePortParameters,
  numPorts:  Range.Inclusive = 0 to 999)
  extends AdapterNode(AXI4Imp)(masterFn, slaveFn, numPorts)

// Nodes passed from an inner module
case class AXI4OutputNode() extends OutputNode(AXI4Imp)
case class AXI4InputNode() extends InputNode(AXI4Imp)

// Nodes used for external ports
case class AXI4BlindOutputNode(portParams: Seq[AXI4SlavePortParameters]) extends BlindOutputNode(AXI4Imp)(portParams)
case class AXI4BlindInputNode(portParams: Seq[AXI4MasterPortParameters]) extends BlindInputNode(AXI4Imp)(portParams)

case class AXI4InternalOutputNode(portParams: Seq[AXI4SlavePortParameters]) extends InternalOutputNode(AXI4Imp)(portParams)
case class AXI4InternalInputNode(portParams: Seq[AXI4MasterPortParameters]) extends InternalInputNode(AXI4Imp)(portParams)

object AXI4AsyncImp extends NodeImp[AXI4AsyncMasterPortParameters, AXI4AsyncSlavePortParameters, AXI4AsyncEdgeParameters, AXI4AsyncEdgeParameters, AXI4AsyncBundle]
{
  def edgeO(pd: AXI4AsyncMasterPortParameters, pu: AXI4AsyncSlavePortParameters) = AXI4AsyncEdgeParameters(pd, pu)
  def edgeI(pd: AXI4AsyncMasterPortParameters, pu: AXI4AsyncSlavePortParameters) = AXI4AsyncEdgeParameters(pd, pu)

  def bundleO(ei: AXI4AsyncEdgeParameters) = new AXI4AsyncBundle(ei.bundle)
  def bundleI(eo: AXI4AsyncEdgeParameters) = new AXI4AsyncBundle(eo.bundle)

  def colour = "#ff0000" // red
  override def labelI(ei: AXI4AsyncEdgeParameters) = ei.slave.depth.toString
  override def labelO(eo: AXI4AsyncEdgeParameters) = eo.slave.depth.toString

  override def mixO(pd: AXI4AsyncMasterPortParameters, node: OutwardNode[AXI4AsyncMasterPortParameters, AXI4AsyncSlavePortParameters, AXI4AsyncBundle]): AXI4AsyncMasterPortParameters  =
   pd.copy(base = pd.base.copy(masters = pd.base.masters.map  { c => c.copy (nodePath = node +: c.nodePath) }))
  override def mixI(pu: AXI4AsyncSlavePortParameters, node: InwardNode[AXI4AsyncMasterPortParameters, AXI4AsyncSlavePortParameters, AXI4AsyncBundle]): AXI4AsyncSlavePortParameters =
   pu.copy(base = pu.base.copy(slaves  = pu.base.slaves.map { m => m.copy (nodePath = node +: m.nodePath) }))
}

case class AXI4AsyncSourceNode(sync: Int)
  extends MixedAdapterNode(AXI4Imp, AXI4AsyncImp)(
    dFn = { p => AXI4AsyncMasterPortParameters(p) },
    uFn = { p => p.base.copy(minLatency = sync+1) }) // discard cycles in other clock domain

case class AXI4AsyncSinkNode(depth: Int, sync: Int)
  extends MixedAdapterNode(AXI4AsyncImp, AXI4Imp)(
    dFn = { p => p.base },
    uFn = { p => AXI4AsyncSlavePortParameters(depth, p) })

case class AXI4AsyncInputNode() extends InputNode(AXI4AsyncImp)
case class AXI4AsyncOutputNode() extends InputNode(AXI4AsyncImp)
