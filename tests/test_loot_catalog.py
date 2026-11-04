"""Probability regressions independent of game startup and generated catalog snapshots."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('catalog',Path(__file__).resolve().parents[1]/'tools/build_loot_chances.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def item(**extra):return {'type':'minecraft:item','name':'test:item',**extra}
def pool(entries=None,**extra):return {'entries':entries or [item()],'rolls':1,**extra}
def build(*pools,**extra):return m.catalog({'test:entities/mob':{'pools':list(pools),**extra}})
def row(result):return result[0].get('test:entities/mob',{}).get('test:item')

class CatalogChecks(unittest.TestCase):
    def test_entry_gate_not_double_counted(self):
        r=build(pool([item(conditions=[{'condition':'minecraft:random_chance','chance':.01}])]))
        self.assertEqual(row(r),[100,100,.01])
    def test_zero_count_is_not_guaranteed(self):
        r=build(pool([item(functions=[{'function':'minecraft:set_count','count':{'min':0,'max':3}}])],rolls=2))
        self.assertAlmostEqual(row(r)[1],93.75)
        self.assertEqual(row(r)[0],200) # EMI selection mean is not count or chance.
    def test_set_and_add_order(self):
        self.assertEqual(m.positive_count([{'function':'set_count','count':0},{'function':'set_count','count':1,'add':True}]),1)
        self.assertEqual(m.positive_count([{'function':'set_count','count':1},{'function':'set_count','count':0}]),0)
    def test_looting_zero_count_keeps_symbolic_model(self):
        fs=[{'function':'set_count','count':{'min':0,'max':1}},{'function':'looting_enchant','count':{'min':0,'max':1}}]
        r=build(pool([item(functions=fs)]))
        self.assertIsNone(row(r));self.assertEqual(r[1]['test:entities/mob']['test:item'][0]['functions'],fs)
    def test_conditional_siblings_change_denominator(self):
        r=build(pool([item(conditions=[{'condition':'random_chance','chance':.5}]),{'type':'minecraft:empty'}]))
        self.assertIsNone(row(r))
    def test_repeated_entry_gate_not_single_factor(self):
        self.assertIsNone(row(build(pool([item(conditions=[{'condition':'random_chance','chance':.5}])],rolls=2))))
    def test_table_function_and_custom_namespace(self):
        self.assertIsNone(row(build(pool(),functions=[{'function':'test:set_count','count':0}])))
        self.assertEqual(row(build(pool(),functions=[{'function':'set_count','count':0}]))[1],0)
    def test_duplicate_skipped_pool_stays_ambiguous(self):
        self.assertIsNone(row(build(pool(),pool(bonus_rolls=1))))
    def test_binomial_draws(self):
        self.assertAlmostEqual(row(build(pool(rolls={'type':'binomial','n':2,'p':.5})))[1],75)

if __name__=='__main__':unittest.main()
