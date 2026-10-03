"""Observer controls: pending public packets cannot authorize reproduction."""
import unittest
from veritrail_m6_installed import published_result

class PublicationControls(unittest.TestCase):
    def test_pending_object_is_not_a_formal_result(self):
        self.assertFalse(published_result({'phase':'FROZEN','result':{}}))

    def test_nonempty_incomplete_or_nonobject_result_is_rejected(self):
        for value in (None,[],{'proof':{}},{'proof':{'round':'1'}}):
            with self.assertRaises(ValueError):published_result({'result':value})

    def test_complete_result_is_only_ready_for_independent_validation(self):
        result=dict.fromkeys(('input_hash','output_hash','outputBytes','maximum_count','candidate_count','published_at'),'synthetic')
        result['proof']={'round':1}
        self.assertTrue(published_result({'result':result}))

if __name__=='__main__':unittest.main()
